/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.allOverridden
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.backend.common.ir.isStatic
import org.jetbrains.kotlin.backend.common.lower.SpecialMethodWithDefaultInfo
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

/*
 * Generate bridge methods to fix virtual dispatch after type erasure and to adapt Kotlin collections to
 * the Java collection interfaces. For example, consider the following Kotlin declaration
 *
 *     interface I<T> { fun f(): T }
 *     abstract class A : MutableCollection<Int>, I<String> {
 *         override fun f(): String = "OK"
 *         override fun contains(o: Int): Boolean = false
 *     }
 *
 * After type erasure we essentially have the following definitions.
 *
 *    interface I { fun f(): java.lang.Object }
 *    abstract class A : java.util.Collection, I {
 *        fun f(): java.lang.String = "OK"
 *        fun contains(o: Int): Boolean = false
 *    }
 *
 * In particular, method `A.f` no longer overrides method `I.f`, since the return types do not match.
 * This is why we have to introduce a bridge method into `A.f` to redirect calls from `I.f` to `A.f` and
 * to add type casts as needed.
 *
 * The second source of bridge methods in Kotlin are so-called special bridges, which mediate between
 * the Kotlin and Java collection interfaces. Note that we map the type `MutableCollection` to its
 * Java equivalent `java.util.Collection`. However, there is a mismatch in naming conventions and
 * signatures between the Java and Kotlin version. For example, the method `contains` has signature
 *
 *     interface kotlin.Collection<T> {
 *         fun contains(element: T): Boolean
 *         ...
 *     }
 *
 * in Kotlin, but a different signature
 *
 *     interface java.util.Collection<T> {
 *         fun contains(element: java.lang.Object): Boolean
 *         ...
 *     }
 *
 * in Java. In particular, the Java version is not type-safe: it requires us to implement the method
 * given arbitrary objects, even though we know based on the types that our collection can only contain
 * members of type `T`. This is why we have to introduce type-safe wrappers into Kotlin collection classes.
 * In the example above, we produce:
 *
 *    abstract class A : java.util.Collection, I {
 *        ...
 *        fun contains(element: java.lang.Object): Boolean {
 *            if (element !is Int) return false
 *            return contains(element as Int)
 *        }
 *
 *        fun contains(o: Int): Boolean = false
 *    }
 *
 * Similarly, the naming conventions sometimes differ between the Java interfaces and their Kotlin counterparts.
 * Sticking with the example above, we find that `java.util.Collection` contains a method `fun size(): Int`,
 * which maps to a Kotlin property `val size: Int`. The latter is compiled to a method `fun getSize(): Int` and
 * we introduce a bridge to map calls from `size()` to `getSize()`.
 *
 * Finally, while bridges due to type erasure are marked as synthetic, we need special bridges to be visible to
 * the Java compiler. After all, special bridges are the implementation methods for some Java interfaces. If
 * they were synthetic, they would be invisible to javac and it would complain that a Kotlin collection implementation
 * class does not implement all of its interfaces. Similarly, special bridges should be final, since otherwise
 * a user coming from Java might override their implementation, leading to the Kotlin and Java collection
 * implementations getting out of sync.
 *
 * In the other direction, it is possible that a user would reimplement a Kotlin collection in Java.
 * In order to guarantee binary compatibility, we remap all calls to Kotlin collection methods to
 * their Java equivalents instead.
 *
 * Apart from these complications, bridge generation is conceptually simple: For a given Kotlin method we
 * generate bridges for all overridden methods with different signatures, unless a final method with
 * the same signature already exists in a superclass. We only diverge from this idea to match the behavior of
 * the JVM backend in a few corner cases.
 */
internal val bridgePhase = makeIrFilePhase(
    ::BridgeLowering,
    name = "Bridge",
    description = "Generate bridges",
    prerequisite = setOf(jvmInlineClassPhase)
)

internal class BridgeLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    // Represents a synthetic bridge to `overridden` with a precomputed signature
    private class Bridge(
        val overridden: IrSimpleFunction,
        val signature: Method,
        val overriddenSymbols: MutableList<IrSimpleFunctionSymbol> = mutableListOf()
    )

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        // Bridges in DefaultImpl classes are handled in InterfaceLowering.
        if (declaration.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS || declaration.isAnnotationClass)
            return super.visitClass(declaration)

        val bridgeTargets = declaration.functions.filterTo(SmartList()) { it.isPotentialBridgeTarget() }
        if (bridgeTargets.isEmpty())
            return super.visitClass(declaration)

        bridgeTargets.forEach { createBridges(declaration, it) }

        if (declaration.isSingleFieldValueClass) {
            // Inline class (implementing 'MutableCollection<T>', where T is Int or an inline class mapped to Int)
            // can contain a static replacement for a function 'remove', which forces value parameter boxing
            // in order to avoid signature clash with 'remove(int)' method in 'java.util.List'.
            // We should rewrite this static replacement as well ('remove' function itself is handled during special bridge processing).
            val remove = declaration.functions.find {
                val original = context.inlineClassReplacements.originalFunctionForStaticReplacement[it]
                original != null && context.methodSignatureMapper.shouldBoxSingleValueParameterForSpecialCaseOfRemove(original)
            }
            if (remove != null) {
                makeLastParameterNullable(remove)
            }
        }

        return super.visitClass(declaration)
    }

    private fun IrSimpleFunction.isPotentialBridgeTarget(): Boolean {
        // Only overrides may need bridges and so in particular, private and static functions do not.
        // Note that this includes the static replacements for inline class functions (which are static, but have
        // overriddenSymbols in order to produce correct signatures in the type mapper).
        if (DescriptorVisibilities.isPrivate(visibility) || isStatic || overriddenSymbols.isEmpty())
            return false

        // None of the methods of Any have type parameters and so we will not need bridges for them.
        if (isMethodOfAny())
            return false

        // We don't produce bridges for abstract functions in interfaces.
        if (isJvmAbstract(context.state.jvmDefaultMode)) {
            if (parentAsClass.isJvmInterface) {
                // If function requires a special bridge, we should record it for generic signatures generation.
                if (specialBridgeOrNull != null) {
                    context.functionsWithSpecialBridges.add(this)
                }
                return false
            }
            return true
        }

        // Finally, the JVM backend also ignores concrete fake overrides whose implementation is directly inherited from an interface.
        // This is sound, since we do not generate type-specialized versions of fake overrides and if the method
        // were to override several interface methods the frontend would require a separate implementation.
        return !isFakeOverride || resolvesToClass()
    }

    private fun makeLastParameterNullable(irFunction: IrSimpleFunction) {
        val oldValueParameter = irFunction.valueParameters.last()
        val newValueParameter = oldValueParameter.copyTo(irFunction, type = oldValueParameter.type.makeNullable())
        irFunction.valueParameters = irFunction.valueParameters.dropLast(1) + newValueParameter
        irFunction.body?.transform(VariableRemapper(mapOf(oldValueParameter to newValueParameter)), null)
    }

    private fun createBridges(irClass: IrClass, irFunction: IrSimpleFunction) {
        // Track final overrides and bridges to avoid clashes
        val blacklist = mutableSetOf<Method>()

        // Don't generate bridges for default argument stubs. This is a workaround for a
        // frontend bug (KT-36188). Ideally, the frontend should not allow inheriting from
        // multiple different default argument stubs, but for now we need this special case
        // to avoid a ClassCastException in the inliner (KT-46389).
        val targetFunction = irFunction.resolveFakeOverride() ?: irFunction
        if (targetFunction.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            return
        }
        // Add the current method to the blacklist if it is concrete or final
        val targetMethod = targetFunction.jvmMethod
        if (!irFunction.isFakeOverride || irFunction.modality == Modality.FINAL)
            blacklist += targetMethod

        // Generate special bridges
        val specialBridge = irFunction.specialBridgeOrNull
        var bridgeTarget = irFunction
        if (specialBridge != null) {
            // If the current function overrides a special bridge then it's possible that we already generated a final
            // bridge methods in a superclass.
            blacklist += irFunction.allOverridden().flatMapTo(arrayListOf()) { it.getSpecialBridgeSignatures() }

            fun getSpecialBridgeTargetAddingExtraBridges(): IrSimpleFunction {
                // We only generate a special bridge method if it does not clash with a final method in a superclass or the current method
                if (specialBridge.signature in blacklist ||
                    irFunction.isFakeOverride && irFunction.jvmMethod == specialBridge.signature
                ) {
                    return irFunction
                }

                if (irFunction.isFakeOverride) {
                    // If irFunction is a fake override, we replace it with a stub and redirect all calls to irFunction with calls to the stub
                    // instead. Otherwise, we'll end up calling the special method itself and get into an infinite loop.
                    bridgeTarget = when {
                        irFunction.isJvmAbstract(context.state.jvmDefaultMode) -> {
                            // If the method is abstract, then we simply generate a concrete abstract method
                            // to avoid generating a call to a method which does not exist in the current class.
                            irClass.declarations.remove(irFunction)
                            irClass.addAbstractMethodStub(irFunction)
                        }
                        irFunction.modality != Modality.FINAL -> {
                            // If we have a non-abstract, non-final fake-override we need to put in an additional bridge which uses
                            // INVOKESPECIAL to call the special bridge implementation in the superclass.
                            // We can be sure that an implementation exists in a superclass,
                            // since we do not generate bridges for fake overrides of interface methods.
                            val overriddenFromClass = irFunction.overriddenFromClass()!!
                            val superBridge = SpecialBridge(
                                overridden = irFunction,
                                signature = irFunction.jvmMethod,
                                superQualifierSymbol = overriddenFromClass.parentAsClass.symbol,
                                methodInfo = specialBridge.methodInfo?.copy(argumentsToCheck = 0), // For potential argument boxing
                                isFinal = false,
                            )

                            // The part after '?:' is needed for methods with default implementations in collection interfaces:
                            // MutableMap.remove() and getOrDefault().
                            val superTarget = overriddenFromClass.takeIf { !it.isFakeOverride || !specialBridge.isOverriding }
                                ?: specialBridge.overridden

                            if (superBridge.signature == superTarget.jvmMethod) {
                                // If the resulting bridge to a super member matches the signature of the bridge callee,
                                // bridge is not needed.
                                irFunction
                            } else {
                                irClass.declarations.remove(irFunction)
                                irClass.addSpecialBridge(superBridge, superTarget)
                            }
                        }
                        else -> {
                            // If the method is final,
                            // then we will not override it in a subclass and we do not need to generate an additional stub method.
                            irFunction
                        }
                    }

                    blacklist += bridgeTarget.jvmMethod
                }

                // Add special bridge with unsubstituted signature (as generated by old JVM back-end) if required.
                // According to KT-40277 we generate special bridges for 'get', 'getOrDefault', and 'remove' with substituted return type.
                // However, this introduces performance regressions and changes in behavior if cases like KT-48945
                // (where a Kotlin class implements a read-only collection interface and extends a Java collection class).
                val unsubstitutedSpecialBridge = specialBridge.unsubstitutedSpecialBridge
                if (unsubstitutedSpecialBridge != null &&
                    unsubstitutedSpecialBridge.signature !in blacklist &&
                    irClass.functions.none { it.isClashingWithPotentialBridge(irFunction.name, unsubstitutedSpecialBridge.signature) }
                ) {
                    blacklist += unsubstitutedSpecialBridge.signature
                    // TODO what should be the special bridge target if we CAN NOT add a substituted special bridge
                    //  (below, clashes with existing method),
                    //  but CAN add an unsubstituted special bridge (here)?
                    irClass.addSpecialBridge(unsubstitutedSpecialBridge, bridgeTarget)
                }

                if (irClass.functions.none { it.isClashingWithPotentialBridge(irFunction.name, specialBridge.signature) }) {
                    blacklist += specialBridge.signature
                    return irClass.addSpecialBridge(specialBridge, bridgeTarget)
                }

                return irFunction
            }

            val specialBridgeTarget = getSpecialBridgeTargetAddingExtraBridges()

            // Deal with existing function that override special bridge methods.
            if (!irFunction.isFakeOverride && specialBridge.methodInfo != null) {
                irFunction.rewriteSpecialMethodBody(targetMethod, specialBridge.signature, specialBridge.methodInfo!!)
            }

            // For generic special bridge methods we need to generate bridges for generic overrides coming from Java or Kotlin interfaces.
            if (specialBridge.substitutedReturnType != null) {
                for (overriddenSpecialBridge in irFunction.overriddenSpecialBridges()) {
                    if (overriddenSpecialBridge.signature !in blacklist) {
                        irClass.addSpecialBridge(overriddenSpecialBridge, specialBridgeTarget)
                        blacklist += overriddenSpecialBridge.signature
                    }
                }
            }
        } else if (irFunction.isJvmAbstract(context.state.jvmDefaultMode)) {
            // Do not generate bridge methods for abstract methods which do not override a special bridge method.
            // This matches the behavior of the JVM backend, but it does mean that we generate superfluous bridges
            // for abstract methods overriding a special bridge for which we do not create a bridge due to,
            // e.g., signature clashes.
            return
        }

        // For concrete fake overrides, some bridges may be inherited from the super-classes. Specifically, bridges for all
        // declarations that are reachable from all concrete immediate super-functions of the given function. Note that all such bridges are
        // guaranteed to delegate to the same implementation as bridges for the given function, that's why it's safe to inherit them.
        //
        // This can still break binary compatibility, but it matches the behavior of the JVM backend.
        if (irFunction.isFakeOverride) {
            for (overriddenSymbol in irFunction.overriddenSymbols) {
                val override = overriddenSymbol.owner
                if (override.isJvmAbstract(context.state.jvmDefaultMode)) continue
                override.allOverridden()
                    .filter { !it.isFakeOverride }
                    .mapTo(blacklist) { it.jvmMethod }
            }
        }

        // Generate common bridges
        val generated = mutableMapOf<Method, Bridge>()

        for (override in irFunction.allOverridden()) {
            if (override.isFakeOverride) continue

            val signature = override.jvmMethod
            if (targetMethod != signature && signature !in blacklist) {
                val bridge = generated.getOrPut(signature) {
                    Bridge(override, signature)
                }
                bridge.overriddenSymbols += override.symbol
            }
        }

        if (generated.isEmpty())
            return

        generated.values
            .filter { it.signature !in blacklist }
            .forEach { irClass.addBridge(it, bridgeTarget) }
    }

    private fun IrSimpleFunction.isClashingWithPotentialBridge(name: Name, signature: Method): Boolean =
        (!this.isFakeOverride || this.modality == Modality.FINAL) && this.name == name && this.jvmMethod == signature

    // Returns the special bridge overridden by the current methods if it exists.
    private val IrSimpleFunction.specialBridgeOrNull: SpecialBridge?
        get() = context.bridgeLoweringCache.computeSpecialBridge(this)

    private fun IrSimpleFunction.getSpecialBridgeSignatures(): List<Method> {
        // Ignore special bridges in interfaces or Java classes. While we never generate special bridges in Java
        // classes, we may generate special bridges in interfaces for methods annotated with @JvmDefault.
        // However, these bridges are not final and are thus safe to override.
        // This matches the behavior of the JVM backend, but it's probably a bad idea since this is an
        // opportunity for a Java and Kotlin implementation of the same interface to go out of sync.

        if (this.parentAsClass.isInterface || this.isFromJava())
            return emptyList()
        val specialBridge = this.specialBridgeOrNull
            ?: return emptyList()

        val result = SmartList<Method>()
        val jvmMethod = this.jvmMethod
        if (specialBridge.signature != jvmMethod) {
            result.add(specialBridge.signature)
        }
        val unsubstitutedSpecialBridge = specialBridge.unsubstitutedSpecialBridge
        if (unsubstitutedSpecialBridge != null && unsubstitutedSpecialBridge.signature != jvmMethod) {
            result.add(unsubstitutedSpecialBridge.signature)
        }
        return result
    }

    // List of special bridge methods which were not implemented in Kotlin superclasses.
    private fun IrSimpleFunction.overriddenSpecialBridges(): List<SpecialBridge> {
        val targetJvmMethod = context.methodSignatureMapper.mapCalleeToAsmMethod(this)
        return allOverridden()
            .filter { it.parentAsClass.isInterface || it.isFromJava() }
            .mapNotNull { it.specialBridgeOrNull }
            .filter { it.signature != targetJvmMethod }
            .map { it.copy(isFinal = false, isSynthetic = true, methodInfo = null) }
    }

    private fun IrClass.addAbstractMethodStub(irFunction: IrSimpleFunction) =
        addFunction {
            updateFrom(irFunction)
            modality = Modality.ABSTRACT
            origin = JvmLoweredDeclarationOrigin.ABSTRACT_BRIDGE_STUB
            name = irFunction.name
            returnType = irFunction.returnType
            isFakeOverride = false
        }.apply {
            // If the function is a property accessor, we need to mark the abstract stub as a property accessor as well.
            // However, we cannot link in the new function as the new accessor for the property, since there might still
            // be references to the original fake override stub.
            copyCorrespondingPropertyFrom(irFunction)
            dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)
            valueParameters = irFunction.valueParameters.map { param ->
                param.copyTo(this, type = param.type)
            }
            overriddenSymbols = irFunction.overriddenSymbols.toList()
        }

    private fun IrClass.addBridge(bridge: Bridge, target: IrSimpleFunction): IrSimpleFunction =
        addFunction {
            startOffset = this@addBridge.startOffset
            endOffset = this@addBridge.startOffset
            modality = Modality.OPEN
            origin = IrDeclarationOrigin.BRIDGE
            name = Name.identifier(bridge.signature.name)
            returnType = bridge.overridden.returnType.eraseTypeParameters()
            isSuspend = bridge.overridden.isSuspend
        }.apply {
            copyAttributes(target)
            copyParametersWithErasure(this@addBridge, bridge.overridden)

            // If target is a throwing stub, bridge also should just throw UnsupportedOperationException.
            // Otherwise, it might throw ClassCastException when downcasting bridge argument to expected type.
            // See KT-49765
            body = if (target.isThrowingStub()) {
                createThrowingStubBody(context, this)
            } else {
                context.createIrBuilder(symbol, startOffset, endOffset).run {
                    irExprBody(delegatingCall(this@apply, target))
                }
            }

            if (!bridge.overridden.returnType.isTypeParameterWithPrimitiveUpperBound()) {
                // The generated bridge method overrides all of the symbols which were overridden by its overrides.
                // This is technically wrong, but it's necessary to generate a method which maps to the same signature.
                // In case of 'fun foo(): T', where 'T' is a type parameter with primitive upper bound (e.g., 'T : Char'),
                // 'foo' is mapped to 'foo()C', regardless of its overrides.
                val inheritedOverrides = bridge.overriddenSymbols
                    .flatMapTo(mutableSetOf()) { it.owner.overriddenSymbols }
                val redundantOverrides = inheritedOverrides.flatMapTo(mutableSetOf()) {
                    it.owner.allOverridden().map { override -> override.symbol }
                }
                overriddenSymbols = inheritedOverrides.filter { it !in redundantOverrides }
            }
        }

    private fun IrSimpleFunction.isThrowingStub(): Boolean {
        if (this.origin != IrDeclarationOrigin.IR_BUILTINS_STUB &&
            this.origin != IrDeclarationOrigin.BRIDGE &&
            this.origin != IrDeclarationOrigin.BRIDGE_SPECIAL
        ) {
            return false
        }
        val body = this.body as? IrBlockBody ?: return false
        if (body.statements.size != 1) return false
        val irCall = body.statements[0] as? IrCall ?: return false
        return irCall.symbol == context.ir.symbols.throwUnsupportedOperationException
    }

    private fun IrType.isTypeParameterWithPrimitiveUpperBound(): Boolean =
        isTypeParameter() && eraseTypeParameters().isPrimitiveType()

    private fun IrClass.addSpecialBridge(specialBridge: SpecialBridge, target: IrSimpleFunction): IrSimpleFunction =
        addFunction {
            startOffset = this@addSpecialBridge.startOffset
            endOffset = this@addSpecialBridge.startOffset
            modality = if (specialBridge.isFinal) Modality.FINAL else Modality.OPEN
            origin = if (specialBridge.isSynthetic) IrDeclarationOrigin.BRIDGE else IrDeclarationOrigin.BRIDGE_SPECIAL
            name = Name.identifier(specialBridge.signature.name)
            returnType = specialBridge.substitutedReturnType?.eraseToScope(target.parentAsClass)
                ?: specialBridge.overridden.returnType.eraseTypeParameters()
        }.apply {
            context.functionsWithSpecialBridges.add(target)

            copyParametersWithErasure(this@addSpecialBridge, specialBridge.overridden, specialBridge.substitutedParameterTypes)

            body = context.createIrBuilder(symbol, startOffset, endOffset).irBlockBody {
                specialBridge.methodInfo?.let { info ->
                    valueParameters.take(info.argumentsToCheck).forEach {
                        +parameterTypeCheck(it, target.valueParameters[it.index].type, info.defaultValueGenerator(this@apply))
                    }
                }
                +irReturn(delegatingCall(this@apply, target, specialBridge.superQualifierSymbol))
            }

            if (specialBridge.isOverriding) {
                overriddenSymbols = listOf(specialBridge.overridden.symbol)
            }

            if (context.methodSignatureMapper.shouldBoxSingleValueParameterForSpecialCaseOfRemove(this)) {
                makeLastParameterNullable(this)
            }
        }

    private fun IrSimpleFunction.rewriteSpecialMethodBody(
        ourSignature: Method,
        specialOverrideSignature: Method,
        specialOverrideInfo: SpecialMethodWithDefaultInfo
    ) {
        // If there is an existing function that would conflict with a special bridge signature, insert the special bridge
        // code directly as a prelude in the existing method.
        val variableMap = mutableMapOf<IrValueParameter, IrValueParameter>()
        if (specialOverrideSignature == ourSignature) {
            val argumentsToCheck = valueParameters.take(specialOverrideInfo.argumentsToCheck)
            val shouldGenerateParameterChecks = argumentsToCheck.any { !it.type.isNullable() }
            if (shouldGenerateParameterChecks) {
                // Rewrite the body to check if arguments have wrong type. If so, return the default value, otherwise,
                // use the existing function body.
                context.createIrBuilder(symbol).run {
                    body = irBlockBody {
                        // Change the parameter types to be Any? so that null checks are not generated. The checks
                        // we insert here make them superfluous.
                        val newValueParameters = ArrayList(valueParameters)
                        argumentsToCheck.forEach {
                            val parameterType = it.type
                            if (!parameterType.isNullable()) {
                                val newParameter = it.copyTo(this@rewriteSpecialMethodBody, type = parameterType.makeNullable())
                                variableMap[valueParameters[it.index]] = newParameter
                                newValueParameters[it.index] = newParameter
                                +parameterTypeCheck(
                                    newParameter,
                                    parameterType,
                                    specialOverrideInfo.defaultValueGenerator(this@rewriteSpecialMethodBody)
                                )
                            }
                        }
                        valueParameters = newValueParameters
                        // After the checks, insert the original method body.
                        if (body is IrExpressionBody) {
                            +irReturn((body as IrExpressionBody).expression)
                        } else {
                            (body as IrBlockBody).statements.forEach { +it }
                        }
                    }
                }
            }
        } else {
            // If the signature of this method will be changed in the output to take a boxed argument instead of a primitive,
            // rewrite the argument so that code will be generated for a boxed argument and not a primitive.
            valueParameters = valueParameters.mapIndexed { i, p ->
                if (AsmUtil.isPrimitive(context.typeMapper.mapType(p.type)) && ourSignature.argumentTypes[i].sort == Type.OBJECT) {
                    val newParameter = p.copyTo(this, type = p.type.makeNullable())
                    variableMap[p] = newParameter
                    newParameter
                } else {
                    p
                }
            }
        }
        // If any parameters change, remap them in the function body.
        if (variableMap.isNotEmpty()) {
            body?.transform(VariableRemapper(variableMap), null)
        }
    }

    private fun IrBuilderWithScope.parameterTypeCheck(parameter: IrValueParameter, type: IrType, defaultValue: IrExpression) =
        irIfThen(context.irBuiltIns.unitType, irNot(irIs(irGet(parameter), type)), irReturn(defaultValue))

    private fun IrSimpleFunction.copyParametersWithErasure(
        irClass: IrClass,
        from: IrSimpleFunction,
        substitutedParameterTypes: List<IrType>? = null
    ) {
        val visibleTypeParameters = collectVisibleTypeParameters(this)
        // This is a workaround for a bug affecting fake overrides. Sometimes we encounter fake overrides
        // with dispatch receivers pointing at a superclass instead of the current class.
        dispatchReceiverParameter = irClass.thisReceiver?.copyTo(this, type = irClass.defaultType)
        extensionReceiverParameter = from.extensionReceiverParameter?.copyWithTypeErasure(this, visibleTypeParameters)
        valueParameters = if (substitutedParameterTypes != null) {
            from.valueParameters.zip(substitutedParameterTypes).map { (param, type) ->
                param.copyWithTypeErasure(this, visibleTypeParameters, type)
            }
        } else {
            from.valueParameters.map { it.copyWithTypeErasure(this, visibleTypeParameters) }
        }
    }

    private fun IrValueParameter.copyWithTypeErasure(
        target: IrSimpleFunction,
        visibleTypeParameters: Set<IrTypeParameter>,
        substitutedType: IrType? = null
    ): IrValueParameter = copyTo(
        target, IrDeclarationOrigin.BRIDGE,
        startOffset = target.startOffset,
        endOffset = target.endOffset,
        type = (substitutedType?.eraseToScope(visibleTypeParameters) ?: type.eraseTypeParameters()),
        // Currently there are no special bridge methods with vararg parameters, so we don't track substituted vararg element types.
        varargElementType = varargElementType?.eraseToScope(visibleTypeParameters),
        // If the parameter has a default value, replace it with a stub, as if this function is coming from an external dependency.
        // Otherwise it can lead to all sorts of problems, for example this default value can reference private functions from another
        // file, which would rightfully make SyntheticAccessorLowering fail.
        defaultValue = if (defaultValue != null) createStubDefaultValue() else null,
    )

    private fun IrBuilderWithScope.delegatingCall(
        bridge: IrSimpleFunction,
        target: IrSimpleFunction,
        superQualifierSymbol: IrClassSymbol? = null
    ) =
        irCastIfNeeded(
            irCall(target, origin = IrStatementOrigin.BRIDGE_DELEGATION, superQualifierSymbol = superQualifierSymbol).apply {
                for ((param, targetParam) in bridge.explicitParameters.zip(target.explicitParameters)) {
                    putArgument(
                        targetParam,
                        irGet(param).let { argument ->
                            if (param == bridge.dispatchReceiverParameter)
                                argument
                            else
                                irCastIfNeeded(argument, targetParam.type.upperBound)
                        }
                    )
                }
            },
            bridge.returnType.upperBound
        )

    private fun IrBuilderWithScope.irCastIfNeeded(expression: IrExpression, to: IrType): IrExpression =
        if (expression.type == to || to.isAny() || to.isNullableAny()) expression else irImplicitCast(expression, to)

    private val IrFunction.jvmMethod: Method
        get() = context.bridgeLoweringCache.computeJvmMethod(this)
}

// Check whether a fake override will resolve to an implementation in class, not an interface.
private fun IrSimpleFunction.resolvesToClass(): Boolean {
    val overriddenFromClass = overriddenFromClass() ?: return false
    return overriddenFromClass.modality != Modality.ABSTRACT
}

private fun IrSimpleFunction.overriddenFromClass(): IrSimpleFunction? =
    overriddenSymbols.singleOrNull { !it.owner.parentAsClass.isJvmInterface }?.owner
