/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.backend.common.ir.isStatic
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.backend.jvm.ir.eraseTypeParameters
import org.jetbrains.kotlin.backend.jvm.ir.isJvmAbstract
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
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
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.getOrPutNullable
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
    description = "Generate bridges"
)

private class BridgeLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    // Represents a synthetic bridge to `overridden` with a precomputed signature
    private class Bridge(
        val overridden: IrSimpleFunction,
        val signature: Method,
        val overriddenSymbols: MutableList<IrSimpleFunctionSymbol> = mutableListOf()
    )

    // Represents a special bridge to `overridden`. Special bridges are always public and non-synthetic.
    // There are ten type-safe wrappers for different collection methods, which have an additional `methodInfo`
    // field. There is only one special bridge method which uses generic types in Java (MutableList.removeAt)
    // and needs a `specializedReturnType` for its overrides. Finally, we sometimes need to use INVOKESPECIAL
    // to invoke an existing special bridge implementation in a superclass, which is what `superQualifierSymbol`
    // is for.
    private data class SpecialBridge(
        val overridden: IrSimpleFunction,
        val signature: Method,
        val specializedReturnType: IrType? = null,
        val methodInfo: SpecialMethodWithDefaultInfo? = null,
        val superQualifierSymbol: IrClassSymbol? = null,
        val isFinal: Boolean = true,
    )

    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    override fun visitClass(declaration: IrClass): IrStatement {
        // Bridges in DefaultImpl classes are handled in InterfaceLowering.
        if (declaration.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS || declaration.isAnnotationClass)
            return super.visitClass(declaration)

        // We generate bridges directly in the class, so we make a copy of the list of relevant members.
        val potentialBridgeTargets = declaration.functions.filterTo(mutableListOf(), fun(irFunction: IrSimpleFunction): Boolean {
            // Only overrides may need bridges and so in particular, private and static functions do not.
            // Note that this includes the static replacements for inline class functions (which are static, but have
            // overriddenSymbols in order to produce correct signatures in the type mapper).
            if (Visibilities.isPrivate(irFunction.visibility) || irFunction.isStatic || irFunction.overriddenSymbols.isEmpty())
                return false

            // We ignore (fake overrides of) default argument stubs and methods of Any.
            // Default argument stubs only appear in the base class and are synthetic, so there is no reason to produce
            // bridges for them. Similarly, none of the methods of Any have type parameters and so we will not need bridges
            // for them.
            if (irFunction.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER || irFunction.isMethodOfAny())
                return false

            // We don't produce bridges for abstract functions in interfaces.
            if (irFunction.isJvmAbstract)
                return !irFunction.parentAsClass.isJvmInterface

            // Finally, the JVM backend also ignores concrete fake overrides which are implemented in interfaces.
            // This is sound, since we do not generate type-specialized versions of fake overrides and if the method
            // were to override several interface methods the frontend would require a separate implementation.
            //
            // In addition, there are @PlatformDependent methods which only exist on newer JDK versions
            // (MutableMap.remove and getOrDefault). Trying to produce (special) bridges for these methods could
            // result in incorrect bytecode on older JVM versions. However, all such methods are declared
            // in interfaces and thus we don't need a separate check for them.
            return !irFunction.isFakeOverride || !irFunction.resolveFakeOverride()!!.parentAsClass.isJvmInterface
        })

        for (member in potentialBridgeTargets) {
            createBridges(declaration, member)

            // For lambda classes, we move overrides from the `invoke` function to its bridge. This will allow us to avoid boxing
            // the return type of `invoke` in codegen for lambdas with primitive return type.
            if (member.name == OperatorNameConventions.INVOKE && declaration.origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL) {
                member.overriddenSymbols = listOf()
            }
        }

        return super.visitClass(declaration)
    }

    private fun createBridges(irClass: IrClass, irFunction: IrSimpleFunction) {
        // Track final overrides and bridges to avoid clashes
        val blacklist = mutableSetOf<Method>()

        // Add the current method to the blacklist if it is concrete or final.
        val targetMethod = (irFunction.resolveFakeOverride() ?: irFunction).jvmMethod
        if (!irFunction.isFakeOverride || irFunction.modality == Modality.FINAL)
            blacklist += targetMethod

        // Generate special bridges
        val specialBridge = irFunction.specialBridgeOrNull
        var bridgeTarget = irFunction
        if (specialBridge != null) {
            // If the current function overrides a special bridge then it's possible that we already generated a final
            // bridge methods in a superclass.
            blacklist += irFunction.overriddenFinalSpecialBridges()

            // We only generate a special bridge method if it does not clash with a final method in a superclass or the current method
            if (specialBridge.signature !in blacklist && (!irFunction.isFakeOverride || irFunction.jvmMethod != specialBridge.signature)) {
                // If irFunction is a fake override, we replace it with a stub and redirect all calls to irFunction with
                // calls to the stub instead. Otherwise we'll end up calling the special method itself and get into an
                // infinite loop.
                //
                // There are three cases to consider. If the method is abstract, then we simply generate a concrete abstract method
                // to avoid generating a call to a method which does not exist in the current class. If the method is final,
                // then we will not override it in a subclass and we do not need to generate an additional stub method.
                //
                // Finally, if we have a non-abstract, non-final fake-override we need to put in an additional bridge which uses
                // INVOKESPECIAL to call the special bridge implementation in the superclass. We can be sure that an implementation
                // exists in a superclass, since we do not generate bridges for fake overrides of interface methods.
                if (irFunction.isFakeOverride) {
                    bridgeTarget = when {
                        irFunction.isJvmAbstract -> {
                            irClass.declarations.remove(irFunction)
                            irClass.addAbstractMethodStub(irFunction)
                        }
                        irFunction.modality != Modality.FINAL -> {
                            val superTarget = irFunction.overriddenSymbols.first { !it.owner.parentAsClass.isInterface }.owner
                            val superBridge = SpecialBridge(
                                irFunction, irFunction.jvmMethod, superQualifierSymbol = superTarget.parentAsClass.symbol,
                                methodInfo = specialBridge.methodInfo?.copy(argumentsToCheck = 0), // For potential argument boxing
                                isFinal = false,
                            )
                            irClass.declarations.remove(irFunction)
                            irClass.addSpecialBridge(superBridge, superTarget)
                        }
                        else -> irFunction
                    }

                    blacklist += bridgeTarget.jvmMethod
                }

                irClass.addSpecialBridge(specialBridge, bridgeTarget)
                blacklist += specialBridge.signature
            }

            // Deal with existing function that override special bridge methods.
            if (!irFunction.isFakeOverride && specialBridge.methodInfo != null) {
                irFunction.rewriteSpecialMethodBody(targetMethod, specialBridge.signature, specialBridge.methodInfo)
            }
        } else if (irFunction.isJvmAbstract) {
            // Do not generate bridge methods for abstract methods which do not override a special bridge method.
            // This matches the behavior of the JVM backend, but it does mean that we generate superfluous bridges
            // for abstract methods overriding a special bridge for which we do not create a bridge due to,
            // e.g., signature clashes.
            return
        }

        // Generate common bridges
        val generated = mutableMapOf<Method, Bridge>()

        irFunction.allOverridden().filter { !it.isFakeOverride }.forEach { override ->
            val signature = override.jvmMethod
            if (targetMethod != signature && signature !in blacklist) {
                generated.getOrPut(signature) {
                    Bridge(override, signature)
                }.overriddenSymbols += override.symbol
            }
        }

        // For concrete fake overrides, some of the bridges may be inherited from the super-classes. Specifically, bridges for all
        // declarations that are reachable from all concrete immediate super-functions of the given function. Note that all such bridges are
        // guaranteed to delegate to the same implementation as bridges for the given function, that's why it's safe to inherit them.
        //
        // This can still break binary compatibility, but it matches the behavior of the JVM backend.
        if (irFunction.isFakeOverride) {
            irFunction.overriddenSymbols.asSequence().map { it.owner }.filter { !it.isJvmAbstract }.forEach { override ->
                override.allOverridden().mapTo(blacklist) { it.jvmMethod }
            }
        }

        generated.values.filter { it.signature !in blacklist }.forEach { irClass.addBridge(it, bridgeTarget) }
    }

    // Returns the special bridge overridden by the current methods if it exists.
    private val IrSimpleFunction.specialBridgeOrNull: SpecialBridge?
        get() = specialBridgeCache.getOrPutNullable(symbol) {
            val specialMethodInfo = specialBridgeMethods.getSpecialMethodInfo(this)
            return when {
                specialMethodInfo != null ->
                    // Note that there are type-safe special bridges with generic return types in Java (namely Map.getOrDefault,
                    // Map.get, and MutableMap.remove), but the JVM backend does not produce overrides with specialized return
                    // types for them. So for compatibility, neither do we.
                    SpecialBridge(this, jvmMethod, methodInfo = specialMethodInfo)

                specialBridgeMethods.isBuiltInWithDifferentJvmName(this) ->
                    if (returnType.isTypeParameter())
                        SpecialBridge(this, jvmMethod, specializedReturnType = returnType)
                    else
                        SpecialBridge(this, jvmMethod)

                else -> {
                    val overriddenSpecialBridge = overriddenSymbols.asSequence().mapNotNull { it.owner.specialBridgeOrNull }.firstOrNull()
                    if (overriddenSpecialBridge?.specializedReturnType != null) {
                        val specializedSignature = Method(
                            overriddenSpecialBridge.signature.name,
                            context.methodSignatureMapper.mapReturnType(this),
                            overriddenSpecialBridge.signature.argumentTypes
                        )
                        overriddenSpecialBridge.copy(signature = specializedSignature, specializedReturnType = returnType)
                    } else {
                        overriddenSpecialBridge
                    }
                }
            }
        }
    private val specialBridgeMethods = SpecialBridgeMethods(context)
    private val specialBridgeCache = mutableMapOf<IrSimpleFunctionSymbol, SpecialBridge?>()

    private fun IrSimpleFunction.overriddenFinalSpecialBridges(): List<Method> = allOverridden().mapNotNullTo(mutableListOf()) {
        // Ignore special bridges in interfaces or Java classes. While we never generate special bridges in Java
        // classes, we may generate special bridges in interfaces for methods annotated with @JvmDefault.
        // However, these bridges are not final and are thus safe to override.
        //
        // This matches the behavior of the JVM backend, but it's probably a bad idea since this is an
        // opportunity for a Java and Kotlin implementation of the same interface to go out of sync.
        if (it.parentAsClass.isInterface || it.comesFromJava())
            null
        else
            it.specialBridgeOrNull?.signature?.takeIf { bridgeSignature -> bridgeSignature != it.jvmMethod }
    }

    private fun IrClass.addAbstractMethodStub(irFunction: IrSimpleFunction) =
        addFunction {
            updateFrom(irFunction)
            modality = Modality.ABSTRACT
            origin = IrDeclarationOrigin.DEFINED
            name = irFunction.name
            returnType = irFunction.returnType
        }.apply {
            dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)
            extensionReceiverParameter = irFunction.extensionReceiverParameter?.copyTo(this)
            valueParameters = irFunction.valueParameters.map { it.copyTo(this) }
        }

    private fun IrClass.addBridge(bridge: Bridge, target: IrSimpleFunction): IrSimpleFunction =
        addFunction {
            modality = Modality.OPEN
            origin = IrDeclarationOrigin.BRIDGE
            // Internal functions can be overridden by non-internal functions, which changes their names since the names of internal
            // functions are mangled. In order to avoid mangling the name twice we reset the visibility for bridges to internal
            // functions to public and use the mangled name directly.
            visibility = bridge.overridden.visibility.takeUnless { it == Visibilities.INTERNAL } ?: Visibilities.PUBLIC
            name = Name.identifier(bridge.signature.name)
            returnType = bridge.overridden.returnType.eraseTypeParameters()
            isSuspend = bridge.overridden.isSuspend
        }.apply {
            copyParametersWithErasure(this@addBridge, bridge.overridden)
            body = context.createIrBuilder(symbol).run { irExprBody(delegatingCall(this@apply, target)) }

            val redundantOverrides = bridge.overriddenSymbols.flatMapTo(mutableSetOf()) {
                it.owner.allOverridden().map { override -> override.symbol }.asIterable()
            }
            overriddenSymbols = bridge.overriddenSymbols.filter { it !in redundantOverrides }
        }

    private fun IrClass.addSpecialBridge(specialBridge: SpecialBridge, target: IrSimpleFunction): IrSimpleFunction =
        addFunction {
            modality = if (specialBridge.isFinal) Modality.FINAL else Modality.OPEN
            origin = IrDeclarationOrigin.BRIDGE_SPECIAL
            name = Name.identifier(specialBridge.signature.name)
            returnType = specialBridge.specializedReturnType ?: specialBridge.overridden.returnType.eraseTypeParameters()
        }.apply {
            copyParametersWithErasure(
                this@addSpecialBridge,
                specialBridge.overridden,
                specialBridge.methodInfo?.needsArgumentBoxing == true
            )
            body = context.createIrBuilder(symbol).irBlockBody {
                specialBridge.methodInfo?.let { info ->
                    valueParameters.take(info.argumentsToCheck).forEach {
                        +parameterTypeCheck(it, target.valueParameters[it.index].type, info.defaultValueGenerator(this@apply))
                    }
                }
                +irReturn(delegatingCall(this@apply, target, specialBridge.superQualifierSymbol))
            }
            overriddenSymbols += specialBridge.overridden.symbol
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
                                val newParameter = it.copyTo(this@rewriteSpecialMethodBody, type = context.irBuiltIns.anyNType)
                                variableMap.put(valueParameters[it.index], newParameter)
                                newValueParameters[it.index] = newParameter
                                +parameterTypeCheck(
                                    newParameter,
                                    parameterType,
                                    specialOverrideInfo.defaultValueGenerator(this@rewriteSpecialMethodBody)
                                )
                            }
                        }
                        valueParameters = newValueParameters
                        // After the checks, insert the orignal method body.
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

    private fun IrSimpleFunction.copyParametersWithErasure(irClass: IrClass, from: IrSimpleFunction, forceArgumentBoxing: Boolean = false) {
        // This is a workaround for a bug affecting fake overrides. Sometimes we encounter fake overrides
        // with dispatch receivers pointing at a superclass instead of the current class.
        dispatchReceiverParameter = irClass.thisReceiver?.copyTo(this, type = irClass.defaultType)
        extensionReceiverParameter = from.extensionReceiverParameter?.copyWithTypeErasure(this, forceArgumentBoxing)
        valueParameters = from.valueParameters.map { it.copyWithTypeErasure(this, forceArgumentBoxing) }
    }

    private fun IrValueParameter.copyWithTypeErasure(target: IrSimpleFunction, forceBoxing: Boolean = false): IrValueParameter = copyTo(
        target, IrDeclarationOrigin.BRIDGE,
        type =
        // SuspendFunction{N} is Function{N+1} at runtime, thus, when we generate a bridge for suspend callable references,
        // we need to replace the type of its continuation parameter with Any?
        if (target.isSuspend && type.eraseTypeParameters().getClass()
                ?.fqNameWhenAvailable == DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME_RELEASE
        ) context.irBuiltIns.anyNType else type.eraseTypeParameters().let { if (forceBoxing) it.makeNullable() else it },
        varargElementType = varargElementType?.eraseTypeParameters()
    )

    private fun IrBuilderWithScope.delegatingCall(
        bridge: IrSimpleFunction,
        target: IrSimpleFunction,
        superQualifierSymbol: IrClassSymbol? = null
    ) = irCastIfNeeded(irCall(target, origin = IrStatementOrigin.BRIDGE_DELEGATION, superQualifierSymbol = superQualifierSymbol).apply {
        for ((param, targetParam) in bridge.explicitParameters.zip(target.explicitParameters)) {
            putArgument(targetParam, irGet(param).let { argument ->
                if (param == bridge.dispatchReceiverParameter) argument else irCastIfNeeded(argument, targetParam.type)
            })
        }
    }, bridge.returnType)

    private fun IrBuilderWithScope.irCastIfNeeded(expression: IrExpression, to: IrType): IrExpression =
        if (expression.type == to || to.isAny() || to.isNullableAny()) expression else irImplicitCast(expression, to)

    private val IrFunction.jvmMethod: Method
        get() = context.methodSignatureMapper.mapAsmMethod(this)
}

private fun IrDeclaration.comesFromJava() = parentAsClass.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
