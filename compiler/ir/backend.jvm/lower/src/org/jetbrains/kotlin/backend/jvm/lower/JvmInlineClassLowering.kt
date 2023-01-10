/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.DefaultInlineClassesUtils.getInlineClassUnderlyingType
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.popLast

/**
 * Adds new constructors, box, and unbox functions to inline classes as well as replacement
 * functions and bridges to avoid clashes between overloaded function. Changes calls with
 * known types to call the replacement functions.
 *
 * We do not unfold inline class types here. Instead, the type mapper will lower inline class
 * types to the types of their underlying field.
 */
internal class JvmInlineClassLowering(
    context: JvmBackendContext,
    scopeStack: MutableList<ScopeWithIr>,
) : JvmValueClassAbstractLowering(context, scopeStack) {
    override val replacements: MemoizedValueClassAbstractReplacements
        get() = context.inlineClassReplacements

    private val valueMap = mutableMapOf<IrValueSymbol, IrValueDeclaration>()

    override fun addBindingsFor(original: IrFunction, replacement: IrFunction) {
        for ((param, newParam) in original.explicitParameters.zip(replacement.explicitParameters)) {
            valueMap[param.symbol] = newParam
        }
    }

    override fun IrClass.isSpecificLoweringLogicApplicable(): Boolean = isInline || isSealedInline

    private fun buildAdditionalMethodsForSealedInlineClass(declaration: IrClass, constructor: IrConstructor) {
        if (declaration.isChildOfSealedInlineClass() && declaration.isInlineOrSealedInline) {
            if (!declaration.isSealedInline) {
                updateGetterForSealedInlineClassChild(declaration, declaration.defaultType.findTopSealedInlineSuperClass())
                buildSpecializedEqualsMethodIfNeeded(declaration)
            }
            rewriteConstructorForSealedInlineClassChild(declaration, declaration.sealedInlineClassParent(), constructor)
            removeMethodsFromSealedInlineClassChildren(declaration)
        }

        if (declaration.isSealedInline && !declaration.isChildOfSealedInlineClass()) {
            buildPrimaryInlineClassConstructor(declaration, constructor)
            buildBoxFunction(declaration)
            buildUnboxFunction(declaration)

            patchReceiverParameterOfValueGetter(declaration)

            val info = SealedInlineClassInfo.analyze(declaration, context)
            buildIsMethodsForSealedInlineClass(info)
            rewriteMethodsForSealed(info)
            buildSpecializedEqualsMethodForSealed(info)
        }
    }

    /**
     * In case of is checks of sealed inline classes, we generate is-Name functions on top and
     * replace all is checks with calls to these functions.
     */
    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        if (expression.typeOperand.isInlineChildOfSealedInlineClass()) {
            val transformed = super.visitTypeOperator(expression) as IrTypeOperatorCall
            val top = transformed.typeOperand.findTopSealedInlineSuperClass()
            val isCheck = context.inlineClassReplacements.getIsSealedInlineChildFunction(top to transformed.typeOperand.classOrNull!!.owner)
            val underlyingType = transformed.typeOperand.unboxInlineClass()
            val currentScopeSymbol = (currentScope?.irElement as? IrSymbolOwner)?.symbol
                ?: error("${currentScope?.irElement?.render()} is not valid symbol owner")

            when (transformed.operator) {
                IrTypeOperator.INSTANCEOF -> {
                    // if (top != null) false else Top.is-Child(top)
                    with(context.createIrBuilder(currentScopeSymbol)) {
                        return irBlock {
                            val tmp = irTemporary(transformed.argument)
                            +irIfNull(
                                context.irBuiltIns.booleanType, irGet(tmp), irFalse(),
                                irCall(isCheck.symbol).apply {
                                    putValueArgument(0, coerceFromSealedInlineClass(top, irGet(tmp)))
                                }
                            )
                        }
                    }
                }

                IrTypeOperator.NOT_INSTANCEOF -> {
                    // if (top != null) true else Top.is-Child(top).not()
                    with(context.createIrBuilder(currentScopeSymbol)) {
                        return irBlock {
                            val tmp = irTemporary(transformed.argument)
                            +irIfNull(
                                context.irBuiltIns.booleanType, irGet(tmp), irTrue(),
                                irNot(irCall(isCheck.symbol).apply {
                                    putValueArgument(0, coerceFromSealedInlineClass(top, irGet(tmp)))
                                })
                            )
                        }
                    }
                }

                IrTypeOperator.CAST -> {
                    // if (Top.is-Child(top)) top else CCE
                    return generateAsCheckForSealedInlineClass(currentScopeSymbol, transformed, top, isCheck, underlyingType) {
                        irThrow(
                            irCall(this@JvmInlineClassLowering.context.ir.symbols.classCastExceptionCtorString).apply {
                                putValueArgument(
                                    0,
                                    irString("Cannot cast to sealed inline class subclass ${expression.typeOperand.classFqName}")
                                )
                            }
                        )
                    }
                }

                IrTypeOperator.SAFE_CAST -> {
                    // if (Top.is-Child(top)) top else null
                    return generateAsCheckForSealedInlineClass(currentScopeSymbol, transformed, top, isCheck, underlyingType) { irNull() }
                }

                else -> {
                    return transformed
                }
            }
        } else if (expression.typeOperand.isNoinlineChildOfSealedInlineClass()) {
            val transformed = super.visitTypeOperator(expression) as IrTypeOperatorCall
            val top = transformed.typeOperand.findTopSealedInlineSuperClass()
            val currentScopeSymbol = (currentScope?.irElement as? IrSymbolOwner)?.symbol
                ?: error("${currentScope?.irElement?.render()} is not valid symbol owner")

            when (expression.operator) {
                IrTypeOperator.CAST -> {
                    transformed.argument = coerceFromSealedInlineClass(top, expression.argument)
                    return transformed
                }

                IrTypeOperator.SAFE_CAST -> {
                    with(context.createIrBuilder(currentScopeSymbol)) {
                        return irBlock {
                            val tmp = irTemporary(transformed.argument)
                            +irIfNull(
                                transformed.type, irGet(tmp), irNull(),
                                irAs(coerceFromSealedInlineClass(top, irGet(tmp)), transformed.typeOperand)
                            )
                        }
                    }
                }

                IrTypeOperator.INSTANCEOF -> {
                    with(context.createIrBuilder(currentScopeSymbol)) {
                        return irBlock {
                            val tmp = irTemporary(transformed.argument)
                            +irIfNull(
                                transformed.type, irGet(tmp), irFalse(),
                                irIs(coerceFromSealedInlineClass(top, irGet(tmp)), transformed.typeOperand)
                            )
                        }
                    }
                }

                IrTypeOperator.NOT_INSTANCEOF -> {
                    with(context.createIrBuilder(currentScopeSymbol)) {
                        return irBlock {
                            val tmp = irTemporary(transformed.argument)
                            +irIfNull(
                                transformed.type, irGet(tmp), irTrue(),
                                irNotIs(coerceFromSealedInlineClass(top, irGet(tmp)), transformed.typeOperand)
                            )
                        }
                    }
                }

                else -> {
                    return transformed
                }
            }
        }

        return super.visitTypeOperator(expression)
    }

    private fun generateAsCheckForSealedInlineClass(
        currentScopeSymbol: IrSymbol,
        expression: IrTypeOperatorCall,
        top: IrClass,
        isCheck: IrSimpleFunction,
        underlyingType: IrType,
        onFail: IrBuilderWithScope.() -> IrExpression
    ): IrExpression {
        with(context.createIrBuilder(currentScopeSymbol)) {
            return irBlock {
                val tmp = irTemporary(expression.argument)
                +irIfNull(
                    expression.type, irGet(tmp), onFail(),
                    irBlock {
                        val unboxedTmp = irTemporary(coerceFromSealedInlineClass(top, irGet(tmp)))
                        +irWhen(
                            expression.type, listOf(
                                irBranch(
                                    irCall(isCheck.symbol).also { it.putValueArgument(0, irGet(unboxedTmp)) },
                                    coerceInlineClasses(irGet(unboxedTmp), underlyingType, expression.typeOperand)
                                ),
                                irElseBranch(onFail())
                            )
                        )
                    }
                )
            }
        }
    }

    /* For `is` and `as` checks, we cannot use INSTANCEOF and CHECKCAST, since the underlying type of inline child can be `Any`
     * instead, we check, whether it is not noinline first, and then use INSTANCEOF underlying type. Since we do not want to generate
     * these switch-cases for each `is` and `as` check, we generate is-<Child> methods, which we call in `is` and `as` checks.
     *
     * These methods we generate only for inline subclasses, for noinline subclasses we simply use INSTANCEOF.
     */
    private fun buildIsMethodsForSealedInlineClass(info: SealedInlineClassInfo) {
        for (childInfo in info.inlineSubclasses + info.sealedInlineSubclasses.dropLast(1)) {
            val child = childInfo.owner
            val function = context.inlineClassReplacements.getIsSealedInlineChildFunction(info.top to child)
            val underlyingType =
                if (child.isSealedInline) context.irBuiltIns.anyNType
                else child.inlineClassRepresentation?.underlyingType ?: error("${child.render()} is neither inline nor sealed inline")

            with(context.createIrBuilder(function.symbol)) {
                function.body = irBlockBody {
                    fun param(): IrExpression = irGet(function.valueParameters.first())

                    val branches = mutableListOf<IrBranch>()

                    if (!underlyingType.isNullable()) {
                        branches += irBranch(
                            irEqeqeq(param(), irNull()),
                            irReturn(irFalse())
                        )
                    }

                    for (noinline in info.noinlineSubclasses) {
                        branches += irBranch(
                            irIs(param(), noinline.owner.defaultType),
                            irReturn(irFalse())
                        )
                    }

                    branches += irElseBranch(irReturn(irIs(param(), underlyingType)))

                    +irReturn(irWhen(context.irBuiltIns.booleanType, branches))
                }
            }

            info.top.addMember(function)
        }
    }

    // Since we cannot create objects of sealed inline class children, we remove virtual methods from the classfile.
    private fun removeMethodsFromSealedInlineClassChildren(irClass: IrClass) {
        irClass.declarations.removeIf {
            it is IrSimpleFunction &&
                    (it.origin == IrDeclarationOrigin.GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER ||
                            it.origin == IrDeclarationOrigin.DEFINED ||
                            it.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR)
        }
    }

    private fun rewriteConstructorForSealedInlineClassChild(irClass: IrClass, parent: IrClass, irConstructor: IrConstructor) {
        val toCall =
            parent.functions.find { it.origin == JvmLoweredDeclarationOrigin.PRIMARY_CONSTRUCTOR_FOR_SEALED_INLINE_CLASS }
                ?: context.inlineClassReplacements.getReplacementFunction(
                    parent.primaryConstructor
                        ?: parent.constructors.find { it.origin == JvmLoweredDeclarationOrigin.PRIMARY_CONSTRUCTOR_FOR_SEALED_INLINE_CLASS }!!
                )!!
        val newConstructor = context.inlineClassReplacements.getReplacementFunction(irConstructor)!!

        with(context.createIrBuilder(newConstructor.symbol)) {
            newConstructor.body = irBlockBody {
                val res = irTemporary(
                    irCall(toCall.symbol).apply {
                        putValueArgument(0, irGet(newConstructor.valueParameters[0]))
                    }
                )
                moveInitBlocksInto(irClass, newConstructor)
                +irReturn(irGet(res))
            }
        }

        irClass.addMember(newConstructor)
    }

    private fun IrClass.sealedInlineClassParent(): IrClass =
        superTypes.single { it.isInlineClassType() }.classOrNull!!.owner

    private fun patchReceiverParameterOfValueGetter(irClass: IrClass) {
        val getter = irClass.functions.single { it.origin == IrDeclarationOrigin.GETTER_OF_SEALED_INLINE_CLASS_FIELD }
        getter.dispatchReceiverParameter = irClass.thisReceiver?.copyTo(getter, type = irClass.defaultType)

        val field = irClass.fields.single { it.origin == IrDeclarationOrigin.FIELD_FOR_SEALED_INLINE_CLASS }

        with(context.createIrBuilder(getter.symbol)) {
            getter.body = irExprBody(
                irGetField(irGet(getter.dispatchReceiverParameter!!), field)
            )
        }
    }

    // For sealed inline class children we generate getter, which simply calls parent's and casts the result.
    private fun updateGetterForSealedInlineClassChild(irClass: IrClass, top: IrClass) {
        val fieldGetter = irClass.functions.find { it.isInlineClassFieldGetter }
            ?: error("${irClass.render()} has no getter")

        val methodToOverride = top.functions.single { it.origin == IrDeclarationOrigin.GETTER_OF_SEALED_INLINE_CLASS_FIELD }

        require(
            methodToOverride.origin == IrDeclarationOrigin.FAKE_OVERRIDE ||
                    methodToOverride.origin == IrDeclarationOrigin.GETTER_OF_SEALED_INLINE_CLASS_FIELD
        )

        val fakeOverride = irClass.addFunction {
            name = methodToOverride.name
            returnType = methodToOverride.returnType
            updateFrom(methodToOverride)
            origin = IrDeclarationOrigin.FAKE_OVERRIDE
            isFakeOverride = true
        }

        fakeOverride.overriddenSymbols += methodToOverride.symbol
        fakeOverride.dispatchReceiverParameter = irClass.thisReceiver?.copyTo(fakeOverride, type = irClass.defaultType)

        with(context.createIrBuilder(fieldGetter.symbol)) {
            fieldGetter.body = irExprBody(
                irCall(fakeOverride.symbol).apply {
                    dispatchReceiver = irGet(fieldGetter.dispatchReceiverParameter!!)
                }
            )
        }

        // Remove the field, generated by IR generator
        irClass.declarations.removeIf { it is IrField }
    }

    private fun IrClass.isChildOfSealedInlineClass(): Boolean = superTypes.any { it.isInlineClassType() }

    private fun IrClass.isSealedInlineClassOrItsChild(): Boolean = isSealedInline || isChildOfSealedInlineClass()

    override fun transformSimpleFunctionFlat(function: IrSimpleFunction, replacement: IrSimpleFunction): List<IrDeclaration> {
        if (function.modality == Modality.ABSTRACT && function.parentAsClass.isSealedInlineClassOrItsChild()) {
            return emptyList()
        }

        if (function.isFakeOverride) {
            if (function.parentAsClass.isChildOfSealedInlineClass()) {
                return listOf(function)
            } else if (function.parentAsClass.isSealedInline) {
                if (function.isFakeOverrideOfDefaultMethod() &&
                    (function.overriddenInChildren() ||
                            context.state.jvmDefaultMode.isEnabled && context.state.jvmDefaultMode.forAllMethodsWithBody)
                ) {
                    return listOf(function)
                }
            }
        }

        for (parameter in replacement.valueParameters) {
            parameter.transformChildrenVoid()
            parameter.defaultValue?.patchDeclarationParents(replacement)
        }

        allScopes.push(createScope(function))
        replacement.body = function.body?.transform(this, null)?.patchDeclarationParents(replacement)
        allScopes.pop()
        replacement.copyAttributes(function)

        // Don't create a wrapper for functions which are only used in an unboxed context
        // However, sealed inline classes do have overrides.
        if ((function.overriddenSymbols.isEmpty() || replacement.dispatchReceiverParameter != null) &&
            !function.parentAsClass.isSealedInline
        ) return listOf(replacement)

        val bridgeFunction = createBridgeDeclaration(
            function,
            when {
                // If the original function has signature which need mangling we still need to replace it with a mangled version.
                (!function.isFakeOverride || function.findInterfaceImplementation(context.state.jvmDefaultMode) != null) &&
                        function.signatureRequiresMangling() ->
                    replacement.name
                // Since we remove the corresponding property symbol from the bridge we need to resolve getter/setter
                // names at this point.
                replacement.isGetter ->
                    Name.identifier(JvmAbi.getterName(replacement.correspondingPropertySymbol!!.owner.name.asString()))

                replacement.isSetter ->
                    Name.identifier(JvmAbi.setterName(replacement.correspondingPropertySymbol!!.owner.name.asString()))

                else ->
                    function.name
            }
        )

        // Update the overridden symbols to point to their inline class replacements
        bridgeFunction.overriddenSymbols = replacement.overriddenSymbols

        val overriddenFromSealedInline = replacement.overriddenSymbols.find { overridden ->
            overridden.owner.parent.let { it is IrClass && it.isSealedInline }
        }

        // Replace the function body with a wrapper
        if (!bridgeFunction.isFakeOverride || !bridgeFunction.parentAsClass.isInlineOrSealedInline) {
            createBridgeBody(bridgeFunction, replacement)
        } else if (overriddenFromSealedInline != null) {
            // If fake override overrides function from sealed inline class, call the overridden function
            createBridgeBody(replacement, overriddenFromSealedInline.owner)
            // However, if the fake override is overridden, generate when in it, calling its children.
        } else {
            // Fake overrides redirect from the replacement to the original function, which is in turn replaced during interfacePhase.
            createBridgeBody(replacement, bridgeFunction)
        }

        return listOf(replacement, bridgeFunction)
    }

    override fun createBridgeDeclaration(source: IrSimpleFunction, replacement: IrSimpleFunction, mangledName: Name): IrSimpleFunction =
        context.irFactory.buildFun {
            updateFrom(source)
            name = mangledName
            returnType = source.returnType
        }.apply {
            copyParameterDeclarationsFrom(source)
            annotations = source.annotations
            parent = source.parent
            // We need to ensure that this bridge has the same attribute owner as its static inline class replacement, since this
            // is used in [CoroutineCodegen.isStaticInlineClassReplacementDelegatingCall] to identify the bridge and avoid generating
            // a continuation class.
            copyAttributes(source)
        }

    private fun IrSimpleFunction.overriddenInChildren(): Boolean {
        val stack = mutableListOf<IrClassSymbol>()
        stack += parentAsClass.sealedSubclasses
        while (stack.isNotEmpty()) {
            val current = stack.popLast()
            val function = current.functions.find { it.owner.allOverridden().contains(this) }
            if (function != null && !function.owner.isFakeOverride) return true
            stack += current.owner.sealedSubclasses
        }
        return false
    }

    private fun IrSimpleFunction.signatureRequiresMangling() =
        fullValueParameterList.any { it.type.getRequiresMangling() } ||
                context.state.functionsWithInlineClassReturnTypesMangled && returnType.getRequiresMangling()

    // We may need to add a bridge method for inline class methods with static replacements. Ideally, we'd do this in BridgeLowering,
    // but unfortunately this is a special case in the old backend. The bridge method is not marked as such and does not follow the normal
    // visibility rules for bridge methods.
    private fun createBridgeDeclaration(source: IrSimpleFunction, mangledName: Name) =
        context.irFactory.buildFun {
            updateFrom(source)
            name = mangledName
            returnType = source.returnType
        }.apply {
            copyParameterDeclarationsFrom(source)
            annotations = source.annotations
            parent = source.parent
            // We need to ensure that this bridge has the same attribute owner as its static inline class replacement, since this
            // is used in [CoroutineCodegen.isStaticInlineClassReplacementDelegatingCall] to identify the bridge and avoid generating
            // a continuation class.
            copyAttributes(source)
        }

    override val specificMangle: SpecificMangle
        get() = SpecificMangle.Inline
    override val IrType.needsHandling get() = isInlineClassType()
    override fun visitClassNewDeclarationsWhenParallel(declaration: IrDeclaration) = Unit

    override fun visitClassNew(declaration: IrClass): IrClass {
        // The arguments to the primary constructor are in scope in the initializers of IrFields.

        declaration.primaryConstructor?.let {
            replacements.getReplacementFunction(it)?.let { replacement -> addBindingsFor(it, replacement) }
        }

        declaration.transformDeclarationsFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction) {
                withinScope(memberDeclaration) {
                    transformFunctionFlat(memberDeclaration)
                }
            } else {
                memberDeclaration.accept(this, null)
                null
            }
        }

        if (declaration.isSpecificLoweringLogicApplicable()) {
            if (declaration.isSealedInline || declaration.isChildOfSealedInlineClass()) {
                val irConstructor = declaration.primaryConstructor
                    ?: declaration.constructors.single {
                        it.origin == JvmLoweredDeclarationOrigin.PRIMARY_CONSTRUCTOR_FOR_SEALED_INLINE_CLASS
                    }
                buildAdditionalMethodsForSealedInlineClass(declaration, irConstructor)
            } else {
                handleSpecificNewClass(declaration)
            }
        }

        return declaration
    }

    override fun handleSpecificNewClass(declaration: IrClass) {
        require(!declaration.isSealedInlineClassOrItsChild())

        val irConstructor = declaration.primaryConstructor!!

        // The field getter is used by reflection and cannot be removed here unless it is internal.
        declaration.declarations.removeIf {
            (it == irConstructor && declaration.modality != Modality.SEALED) ||
                    (it is IrFunction && it.isInlineClassFieldGetter && !it.visibility.isPublicAPI)
        }

        buildPrimaryInlineClassConstructor(declaration, irConstructor)
        // For sealed inline class subclasses boxing and unboxing is done via top's box-impl and unbox-impl
        buildBoxFunction(declaration)
        buildUnboxFunction(declaration)
        buildSpecializedEqualsMethodIfNeeded(declaration)
    }

    override fun createBridgeBody(source: IrSimpleFunction, target: IrSimpleFunction, returnBoxedSealedInlineClass: Boolean) {
        source.body = context.createIrBuilder(source.symbol, source.startOffset, source.endOffset).run {
            val call = irCall(target).apply {
                passTypeArgumentsFrom(source)
                for ((parameter, newParameter) in source.explicitParameters.zip(target.explicitParameters)) {
                    putArgument(newParameter, irGet(parameter))
                }
            }
            irExprBody(
                if (returnBoxedSealedInlineClass) {
                    coerceToSealedInlineClass(call, source.returnType.getClass()!!)
                } else {
                    call
                }
            )
        }
    }

    // Secondary constructors for boxed types get translated to static functions returning
    // unboxed arguments. We remove the original constructor.
    // Primary constructors' case is handled at the start of transformFunctionFlat
    override fun transformSecondaryConstructorFlat(constructor: IrConstructor, replacement: IrSimpleFunction): List<IrDeclaration> {
        replacement.valueParameters.forEach { it.transformChildrenVoid() }
        replacement.body = context.createIrBuilder(replacement.symbol, replacement.startOffset, replacement.endOffset).irBlockBody(
            replacement
        ) {
            val thisVar = irTemporary(irType = replacement.returnType, nameHint = "\$this")
            valueMap[constructor.constructedClass.thisReceiver!!.symbol] = thisVar

            constructor.body?.statements?.forEach { statement ->
                +statement
                    .transformStatement(object : IrElementTransformerVoid() {
                        // Don't recurse under nested class declarations
                        override fun visitClass(declaration: IrClass): IrStatement {
                            return declaration
                        }

                        // Capture the result of a delegating constructor call in a temporary variable "thisVar".
                        //
                        // Within the constructor we replace references to "this" with references to "thisVar".
                        // This is safe, since the delegating constructor call precedes all references to "this".
                        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                            expression.transformChildrenVoid()
                            return irSet(thisVar.symbol, expression)
                        }

                        // A constructor body has type unit and may contain explicit return statements.
                        // These early returns may have side-effects however, so we still have to evaluate
                        // the return expression. Afterwards we return "thisVar".
                        // For example, the following is a valid inline class declaration.
                        //
                        //     inline class Foo(val x: String) {
                        //       constructor(y: Int) : this(y.toString()) {
                        //         if (y == 0) return throw java.lang.IllegalArgumentException()
                        //         if (y == 1) return
                        //         return Unit
                        //       }
                        //     }
                        override fun visitReturn(expression: IrReturn): IrExpression {
                            expression.transformChildrenVoid()
                            if (expression.returnTargetSymbol != constructor.symbol)
                                return expression

                            return irReturn(irBlock(expression.startOffset, expression.endOffset) {
                                +expression.value
                                +irGet(thisVar)
                            })
                        }
                    })
                    .transformStatement(this@JvmInlineClassLowering)
                    .patchDeclarationParents(replacement)
            }

            +irReturn(irGet(thisVar))
        }

        return listOf(replacement)
    }

    private fun IrMemberAccessExpression<*>.buildReplacement(
        originalFunction: IrFunction,
        original: IrMemberAccessExpression<*>,
        replacement: IrSimpleFunction
    ) {
        copyTypeArgumentsFrom(original)
        val valueParameterMap = originalFunction.explicitParameters.zip(replacement.explicitParameters).toMap()
        for ((parameter, argument) in typedArgumentList(originalFunction, original)) {
            if (argument == null) continue
            val newParameter = valueParameterMap.getValue(parameter)
            putArgument(replacement, newParameter, argument.transform(this@JvmInlineClassLowering, null))
        }
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        if (expression.origin == InlineClassAbi.UNMANGLED_FUNCTION_REFERENCE)
            return super.visitFunctionReference(expression)

        val function = expression.symbol.owner
        val replacement = context.inlineClassReplacements.getReplacementFunction(function)
            ?: return super.visitFunctionReference(expression)

        // In case of callable reference to inline class constructor,
        // type parameters of the replacement include class's type parameters,
        // however, expression does not. Thus, we should not include them either.
        return IrFunctionReferenceImpl(
            expression.startOffset, expression.endOffset, expression.type,
            replacement.symbol, function.typeParameters.size,
            replacement.valueParameters.size, expression.reflectionTarget, expression.origin
        ).apply {
            buildReplacement(function, expression, replacement)
        }.copyAttributes(expression)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        val function = expression.symbol.owner.findTopSealedInlineClassFunction()

        val delegatingCallOrSealedInlineClassConstructorInsideNoinlineClass =
            expression is IrDelegatingConstructorCall && function.parentAsClass.isSealedInline &&
                    (currentClass?.irElement as? IrClass)?.isInlineOrSealedInline == false

        val replacement = context.inlineClassReplacements.getReplacementFunction(function)
            .takeIf { !delegatingCallOrSealedInlineClassConstructorInsideNoinlineClass }
            ?: return super.visitFunctionAccess(expression)

        return IrCallImpl(
            expression.startOffset, expression.endOffset, function.returnType.substitute(expression.typeSubstitutionMap),
            replacement.symbol, replacement.typeParameters.size, replacement.valueParameters.size,
            expression.origin, (expression as? IrCall)?.superQualifierSymbol
        ).apply {
            buildReplacement(function, expression, replacement)
        }
    }

    private fun IrFunction.findTopSealedInlineClassFunction(): IrFunction {
        var function: IrSimpleFunction? = this as? IrSimpleFunction ?: return this

        val inlineClass = (function!!.parent as? IrClass)?.takeIf { it.isInline || it.isSealedInline } ?: return function

        val topSealedInlineClass =
            if (inlineClass.isChildOfSealedInlineClass()) inlineClass.defaultType.findTopSealedInlineSuperClass()
            else null

        if (topSealedInlineClass != null) {
            while (function != null && function.parent != topSealedInlineClass) {
                function = function.overriddenSymbols.find { symbol ->
                    symbol.owner.parentAsClass.let { it.isInline || it.isSealedInline }
                }?.owner
            }
        }

        if (function == null) {
            return context.inlineClassReplacements.getSealedInlineClassChildFunctionInTop(topSealedInlineClass!! to withoutReceiver())
        }

        return function
    }

    private fun coerceInlineClasses(argument: IrExpression, from: IrType, to: IrType, skipCast: Boolean = false): IrExpression {
        return IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, to, context.ir.symbols.unsafeCoerceIntrinsic).apply {
            val underlyingType = from.erasedUpperBound.inlineClassRepresentation?.underlyingType
            if (underlyingType?.isTypeParameter() == true && !skipCast) {
                putTypeArgument(0, from)
                putTypeArgument(1, underlyingType)
                putValueArgument(
                    0, IrTypeOperatorCallImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET, to, IrTypeOperator.IMPLICIT_CAST, underlyingType, argument
                    )
                )
            } else {
                putTypeArgument(0, from)
                putTypeArgument(1, to)
                putValueArgument(0, argument)
            }
        }
    }

    private fun IrExpression.coerceToUnboxed() =
        coerceInlineClasses(this, this.type, this.type.unboxInlineClass())

    // Precondition: left has an inline class type, but may not be unboxed
    private fun IrBuilderWithScope.specializeEqualsCall(left: IrExpression, right: IrExpression): IrExpression? {
        // There's already special handling for null-comparisons in the Equals intrinsic.
        if (left.isNullConst() || right.isNullConst())
            return null

        // We don't specialize calls when both arguments are boxed.
        val leftIsUnboxed = unboxInlineClass(left.type) != left.type
        val rightIsUnboxed = unboxInlineClass(right.type) != right.type
        if (!leftIsUnboxed && !rightIsUnboxed)
            return null

        // Precondition: left is an unboxed inline class type
        fun equals(left: IrExpression, right: IrExpression): IrExpression {
            // Unsigned types use primitive comparisons
            if (left.type.isUnsigned() && right.type.isUnsigned() && rightIsUnboxed)
                return irEquals(left.coerceToUnboxed(), right.coerceToUnboxed())

            val klass = left.type.classOrNull!!.owner
            val equalsMethod = if (rightIsUnboxed) {
                this@JvmInlineClassLowering.context.inlineClassReplacements.getSpecializedEqualsMethod(klass, context.irBuiltIns)
            } else {
                val equals = klass.functions.single { it.name.asString() == "equals" && it.overriddenSymbols.isNotEmpty() }
                this@JvmInlineClassLowering.context.inlineClassReplacements.getReplacementFunction(equals)!!
            }

            return irCall(equalsMethod).apply {
                putValueArgument(0, left)
                putValueArgument(1, right)
            }
        }

        val leftNullCheck = left.type.isNullable()
        val rightNullCheck = rightIsUnboxed && right.type.isNullable() // equals-impl has a nullable second argument
        if (leftNullCheck || rightNullCheck) {
            return irBlock {
                val leftVal = if (left is IrGetValue) left.symbol.owner else irTemporary(left)
                val rightVal = if (right is IrGetValue) right.symbol.owner else irTemporary(right)

                val equalsCall = equals(
                    if (leftNullCheck) irImplicitCast(irGet(leftVal), left.type.makeNotNull()) else irGet(leftVal),
                    if (rightNullCheck) irImplicitCast(irGet(rightVal), right.type.makeNotNull()) else irGet(rightVal)
                )

                val equalsRight = if (rightNullCheck) {
                    irIfNull(context.irBuiltIns.booleanType, irGet(rightVal), irFalse(), equalsCall)
                } else {
                    equalsCall
                }

                if (leftNullCheck) {
                    +irIfNull(context.irBuiltIns.booleanType, irGet(leftVal), irEqualsNull(irGet(rightVal)), equalsRight)
                } else {
                    +equalsRight
                }
            }
        } else if (left.type.isInlineChildOfSealedInlineClass() || left.type.classOrNull?.owner?.isSealedInline == true) {
            return irBlock {
                val leftTop =
                    if (left.type.isInlineChildOfSealedInlineClass()) left.type.findTopSealedInlineSuperClass()
                    else left.type.classOrNull!!.owner

                val rightVar = irTemporary(
                    if (right.type.classOrNull?.owner?.let { it.isSealedInline || it.isChildOfSealedInlineClass() } == true)
                        coerceFromSealedInlineClass(right.type.findTopSealedInlineSuperClass(), right)
                    else right
                )

                val leftVar: IrVariable
                val equalsMethod: IrSimpleFunction
                if (rightIsUnboxed || right.type.isNoinlineChildOfSealedInlineClass()) {
                    leftVar = irTemporary(coerceFromSealedInlineClass(leftTop, left))
                    equalsMethod =
                        this@JvmInlineClassLowering.context.inlineClassReplacements.getSpecializedEqualsMethod(leftTop, context.irBuiltIns)
                } else {
                    leftVar = irTemporary(left)
                    val equals = leftTop.functions.single { it.name.asString() == "equals" && it.overriddenSymbols.isNotEmpty() }
                    equalsMethod = this@JvmInlineClassLowering.context.inlineClassReplacements.getReplacementFunction(equals)!!
                }

                +irCall(equalsMethod).apply {
                    putValueArgument(0, irGet(leftVar))
                    putValueArgument(1, irGet(rightVar))
                }
            }
        } else {
            return equals(left, right)
        }
    }

    private fun unboxInlineClass(type: IrType): IrType? {
        val irClass = type.classOrNull?.owner ?: return null
        return if (irClass.isSealedInline) context.irBuiltIns.anyNType
        else type.unboxInlineClass()
    }

    override fun visitCall(expression: IrCall): IrExpression =
        when {
            // Getting the underlying field of an inline class merely changes the IR type,
            // since the underlying representations are the same.
            expression.symbol.owner.isInlineClassFieldGetter -> {
                val arg = expression.dispatchReceiver!!.transform(this, null)
                val type =
                    if (expression.symbol.owner.parentAsClass.isChildOfSealedInlineClass() && expression.type.isPrimitiveType())
                        expression.type.makeNullable()
                    else expression.type
                coerceInlineClasses(arg, expression.symbol.owner.dispatchReceiverParameter!!.type, type)
            }
            // Specialize calls to equals when the left argument is a value of inline class type.
            expression.isSpecializedInlineClassEqEq || expression.isSpecializedInlineClassEquals -> {
                expression.transformChildrenVoid()
                val leftOp: IrExpression
                val rightOp: IrExpression
                if (expression.isSpecializedInlineClassEqEq) {
                    leftOp = expression.getValueArgument(0)!!
                    rightOp = expression.getValueArgument(1)!!
                } else {
                    leftOp = expression.dispatchReceiver!!
                    rightOp = expression.getValueArgument(0)!!
                }
                context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                    .specializeEqualsCall(leftOp, rightOp)
                    ?: expression
            }

            expression.isSpecializedNoinlineChildOfSealedInlineClassEqEq -> {
                expression.transformChildrenVoid()
                callEqualsOfTopSealedInlineClass(expression) ?: expression
            }

            else ->
                super.visitCall(expression)
        }

    private val IrCall.isSpecializedInlineClassEquals: Boolean
        get() {
            return isSpecializedInlineClassEqualityCheck { symbol.owner.isEquals() }
        }

    private fun callEqualsOfTopSealedInlineClass(expression: IrCall): IrExpression? {
        if (expression.getValueArgument(0)?.isNullConst() == true || expression.getValueArgument(1)?.isNullConst() == true)
            return null

        with(context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)) {
            val top = expression.getValueArgument(0)!!.type.findTopSealedInlineSuperClass()

            return irBlock {
                val left = expression.getValueArgument(0)!!
                val right = expression.getValueArgument(1)!!

                if (right.type.isNoinlineChildOfSealedInlineClass() || right.type.classOrNull?.owner?.isSealedInline == true) {
                    val equals = this@JvmInlineClassLowering.context.inlineClassReplacements.getSpecializedEqualsMethod(
                        top, context.irBuiltIns
                    )
                    +irCall(equals).also {
                        it.putValueArgument(0, coerceFromSealedInlineClass(top, left))
                        it.putValueArgument(1, coerceFromSealedInlineClass(top, right))
                    }
                } else {
                    val equals = this@JvmInlineClassLowering.context.inlineClassReplacements.getReplacementFunction(
                        top.functions.single { it.name.asString() == "equals" && it.overriddenSymbols.isNotEmpty() }
                    )!!

                    +irCall(equals).also {
                        it.putValueArgument(0, left)
                        it.putValueArgument(1, right)
                    }
                }
            }
        }
    }

    private val IrCall.isSpecializedInlineClassEqEq: Boolean
        get() {
            // Note that reference equality (x === y) is not allowed on values of inline class type,
            // so it is enough to check for eqeq.
            return isSpecializedInlineClassEqualityCheck { symbol == context.irBuiltIns.eqeqSymbol }
        }

    private inline fun IrCall.isSpecializedInlineClassEqualityCheck(calleePredicate: (IrSimpleFunctionSymbol) -> Boolean): Boolean {
        if (!calleePredicate(symbol)) return false

        val leftClass = getValueArgument(0)?.type?.classOrNull?.owner?.takeIf { it.isInline || it.isSealedInline }
            ?: return false

        // Before version 1.4, we cannot rely on the Result.equals-impl0 method
        return (leftClass.fqNameWhenAvailable != StandardNames.RESULT_FQ_NAME) ||
                context.state.languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4
    }

    private val IrCall.isSpecializedNoinlineChildOfSealedInlineClassEqEq: Boolean
        get() {
            if (symbol != context.irBuiltIns.eqeqSymbol) return false

            return getValueArgument(0)?.type?.isNoinlineChildOfSealedInlineClass() == true
        }

    override fun visitGetField(expression: IrGetField): IrExpression {
        val field = expression.symbol.owner
        val parent = field.parent
        if (field.origin == IrDeclarationOrigin.PROPERTY_BACKING_FIELD &&
            parent is IrClass &&
            parent.isInline &&
            field.name == parent.inlineClassFieldName
        ) {
            val receiver = expression.receiver!!.transform(this, null)
            val type =
                if (field.parentAsClass.isChildOfSealedInlineClass() && field.type.isPrimitiveType()) field.type.makeNullable()
                else field.type
            // If we get the field of nullable variable, we can be sure, that type is not null,
            // since we first generate null check.
            return coerceInlineClasses(receiver, receiver.type.makeNotNull(), type)
        }
        return super.visitGetField(expression)
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        valueMap[expression.symbol]?.let {
            return IrGetValueImpl(
                expression.startOffset, expression.endOffset,
                it.type, it.symbol, expression.origin
            )
        }
        return super.visitGetValue(expression)
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        valueMap[expression.symbol]?.let {
            return IrSetValueImpl(
                expression.startOffset, expression.endOffset,
                it.type, it.symbol,
                expression.value.transform(this@JvmInlineClassLowering, null),
                expression.origin
            )
        }
        return super.visitSetValue(expression)
    }

    private fun buildPrimaryInlineClassConstructor(valueClass: IrClass, irConstructor: IrConstructor) {
        // Add the default primary constructor
        valueClass.addConstructor {
            updateFrom(irConstructor)
            visibility = if (valueClass.isSealedInline) DescriptorVisibilities.PROTECTED else DescriptorVisibilities.PRIVATE
            origin =
                if (valueClass.isSealedInline) JvmLoweredDeclarationOrigin.PRIMARY_CONSTRUCTOR_FOR_SEALED_INLINE_CLASS
                else JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER
            returnType = irConstructor.returnType
        }.apply {
            if (valueClass.isSealedInline) {
                addValueParameter(
                    InlineClassAbi.sealedInlineClassFieldName,
                    context.irBuiltIns.anyNType,
                    JvmLoweredDeclarationOrigin.PRIMARY_CONSTRUCTOR_PARAMETER_FOR_SEALED_INLINE_CLASS
                )
            }
            // Don't create a default argument stub for the primary constructor
            irConstructor.valueParameters.forEach { it.defaultValue = null }
            copyParameterDeclarationsFrom(irConstructor)
            annotations = irConstructor.annotations
            body = context.createIrBuilder(this.symbol).irBlockBody(this) {
                +irDelegatingConstructorCall(valueClass.superTypes.singleOrNull {
                    it.classOrNull?.owner?.kind == ClassKind.CLASS
                }?.classOrNull?.constructors?.singleOrNull()?.owner ?: context.irBuiltIns.anyClass.owner.constructors.single())
                +irSetField(
                    irGet(valueClass.thisReceiver!!),
                    getInlineClassBackingField(valueClass),
                    irGet(this@apply.valueParameters[0])
                )
            }
        }

        // Add a static bridge method to the primary constructor. This contains
        // null-checks, default arguments, and anonymous initializers.
        val function = context.inlineClassReplacements.getReplacementFunction(irConstructor)!!

        function.valueParameters.forEach { it.transformChildrenVoid() }
        function.body = context.createIrBuilder(function.symbol).irBlockBody {
            val argument = function.valueParameters[0]
            val thisValue = irTemporary(coerceInlineClasses(irGet(argument), argument.type, function.returnType, skipCast = true))
            valueMap[valueClass.thisReceiver!!.symbol] = thisValue
            moveInitBlocksInto(valueClass, function)
            +irReturn(irGet(thisValue))
        }

        valueClass.declarations += function
    }

    private fun IrBlockBodyBuilder.moveInitBlocksInto(
        irClass: IrClass,
        function: IrSimpleFunction
    ) {
        val initBlocks = irClass.declarations.filterIsInstance<IrAnonymousInitializer>()
        for (initBlock in initBlocks) {
            for (stmt in initBlock.body.statements) {
                +stmt.transformStatement(this@JvmInlineClassLowering).patchDeclarationParents(function)
            }
        }
        irClass.declarations.removeAll(initBlocks)
    }

    private fun buildBoxFunction(valueClass: IrClass) {
        val function = context.inlineClassReplacements.getBoxFunction(valueClass)

        val primaryConstructor =
            if (valueClass.isSealedInline) {
                valueClass.declarations.single {
                    it is IrConstructor && it.origin == JvmLoweredDeclarationOrigin.PRIMARY_CONSTRUCTOR_FOR_SEALED_INLINE_CLASS
                } as IrConstructor
            } else valueClass.primaryConstructor!!

        with(context.createIrBuilder(function.symbol)) {
            function.body = irExprBody(
                irCall(primaryConstructor.symbol).apply {
                    passTypeArgumentsFrom(function)
                    putValueArgument(0, irGet(function.valueParameters[0]))
                }
            )
        }
        valueClass.declarations += function
    }

    /**
     * Methods of sealed inline classes and their children all go through the top,
     * where we check, which one we should call in giant switch-case.
     *
     * First, we check for noinline children, then for inline children and finally, just run the top's method body.
     */
    private fun rewriteMethodsForSealed(info: SealedInlineClassInfo) {
        for ((methodSymbol, retargets, addToClass) in info.methods) {
            if (methodSymbol.owner.isEquals()) {
                generateEqualsMethodForSealedInlineClass(info.top, replacements.getReplacementFunction(methodSymbol.owner)!!)

                continue
            }

            val replacements = context.inlineClassReplacements
            val original = methodSymbol.owner.attributeOwnerId as IrSimpleFunction
            val staticReplacement = replacements.getReplacementFunction(original)
                ?: replacements.getReplacementFunction(
                    replacements.getSealedInlineClassChildFunctionInTop(info.top to original.withoutReceiver())
                ) ?: error("Cannot find replacement for ${methodSymbol.owner.render()}")

            with(context.createIrBuilder(staticReplacement.symbol)) {
                staticReplacement.body = irBlockBody {
                    val receiver = irTemporary(coerceFromSealedInlineClass(info.top, irGet(staticReplacement.valueParameters[0])))

                    val branches = mutableListOf<IrBranch>()

                    for (noinlineSubclass in info.noinlineSubclasses) {
                        val retarget = retargets[noinlineSubclass]
                        // No override defined in the child
                        // In this case, we just move the body of the parent
                        if (retarget == null) {
                            branches += irBranch(
                                irIs(
                                    coerceFromSealedInlineClass(info.top, irGet(staticReplacement.valueParameters[0])),
                                    noinlineSubclass.owner.defaultType
                                ),
                                sealedInlineClassMethodDefaultBehavior(info.top, methodSymbol, staticReplacement)
                            )
                            continue
                        }
                        val retargetClass = retarget.owner.parentAsClass
                        val toCall =
                            if (retargetClass.let { it.isInline || it.isSealedInline })
                                replacements.getReplacementFunction(retarget.owner)!!.symbol
                            else retarget

                        branches +=
                            irBranch(
                                irIs(
                                    coerceFromSealedInlineClass(info.top, irGet(staticReplacement.valueParameters[0])),
                                    noinlineSubclass.owner.defaultType
                                ),
                                if (retargetClass.symbol == info.top.symbol)
                                    sealedInlineClassMethodDefaultBehavior(info.top, methodSymbol, staticReplacement)
                                else irCall(toCall).apply {
                                    if (retargetClass.isInlineOrSealedInline) {
                                        putValueArgument(0, irGet(staticReplacement.valueParameters[0]))
                                    } else {
                                        dispatchReceiver =
                                            irImplicitCast(irGet(staticReplacement.valueParameters[0]), noinlineSubclass.owner.defaultType)
                                    }
                                    for ((target, source) in toCall.owner.explicitParameters.zip(staticReplacement.explicitParameters)
                                        .drop(1)) {
                                        putArgument(target, irGet(source))
                                    }
                                }
                            )
                    }

                    for (inlineSubclass in info.inlineSubclasses) {
                        val underlyingType = inlineSubclass.owner.inlineClassRepresentation!!.underlyingType

                        val retarget = retargets[inlineSubclass] ?: continue
                        val retargetClass = retarget.owner.parentAsClass
                        val toCall = replacements.getReplacementFunction(retarget.owner)!!.symbol

                        branches +=
                            irBranch(
                                irIs(irGet(receiver), underlyingType),
                                if (retargetClass.symbol == info.top.symbol)
                                    sealedInlineClassMethodDefaultBehavior(info.top, methodSymbol, staticReplacement)
                                else irCall(toCall).apply {
                                    putValueArgument(
                                        0, coerceInlineClasses(irGet(receiver), underlyingType, inlineSubclass.owner.defaultType)
                                    )
                                    for ((target, source) in toCall.owner.explicitParameters.zip(staticReplacement.explicitParameters)
                                        .drop(1)) {
                                        putArgument(target, irGet(source))
                                    }
                                }
                            )
                    }

                    val sealedInlineSubclass = info.sealedInlineSubclasses.first()
                    var retarget = retargets[sealedInlineSubclass]
                    if (retarget != null && retarget.owner.origin != JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT) {
                        retarget = replacements.getReplacementFunction(retarget.owner)?.symbol
                    }

                    branches += irElseBranch(
                        if (retarget != null && retarget.owner.parentAsClass.symbol != info.top.symbol) {
                            irCall(retarget).apply {
                                putValueArgument(0, coerceToSealedInlineClass(irGet(receiver), info.top))
                                for ((target, source) in retarget.owner.explicitParameters
                                    .zip(staticReplacement.explicitParameters).drop(1)
                                ) {
                                    putArgument(target, irGet(source))
                                }
                            }
                        } else {
                            sealedInlineClassMethodDefaultBehavior(info.top, methodSymbol, staticReplacement)
                        }
                    )

                    +irReturn(irWhen(staticReplacement.returnType, branches))
                }
            }

            if (addToClass) {
                info.top.addFunctionToSealedInlineClass(methodSymbol, staticReplacement, retargets)
            }
        }
    }

    // Default behavior of can be either
    // 1. Execute existing body (oldBody)
    // 2. Call super method
    // If both these fail, throw an exception, which should be unreachable, unless it is called via reflection.
    private fun IrBlockBodyBuilder.sealedInlineClassMethodDefaultBehavior(
        irClass: IrClass,
        methodSymbol: IrSimpleFunctionSymbol,
        staticReplacement: IrSimpleFunction
    ): IrExpression = when (val oldBody = staticReplacement.body) {
        is IrExpressionBody -> irReturn(oldBody.expression.deepCopySavingMetadata(staticReplacement))
        is IrBlockBody -> irBlock {
            for (stmt in oldBody.statements) {
                +stmt.deepCopySavingMetadata(staticReplacement)
            }
        }

        null -> {
            callDefaultMethodOfSealedInlineClassSuperinterface(irClass, methodSymbol, staticReplacement)
                ?: irThrow(
                    irCall(this@JvmInlineClassLowering.context.ir.symbols.illegalStateExceptionCtorString).apply {
                        putValueArgument(0, irString("Invalid sealed inline class method call"))
                    }
                )
        }

        else -> error("oldBody is not a function body: ${oldBody.dump()}")
    }

    private fun IrClass.addFunctionToSealedInlineClass(
        methodSymbol: IrSimpleFunctionSymbol,
        staticReplacement: IrSimpleFunction,
        retargets: Map<IrClassSymbol, IrSimpleFunctionSymbol>
    ) {
        if (methodSymbol.owner.modality != Modality.ABSTRACT) {
            addMember(staticReplacement)
        }

        createBridgeToSealedInlineClassMethodIfNeeded(methodSymbol, staticReplacement, retargets)
        updateOverriddenSymbolsOfSealedInlineClassMethodOverrides(methodSymbol, staticReplacement, retargets.values)
        removeFakeOverrideOfDefaultMethodFromSealedInlineClass(methodSymbol)
    }

    private fun updateOverriddenSymbolsOfSealedInlineClassMethodOverrides(
        methodSymbol: IrSimpleFunctionSymbol,
        staticReplacement: IrSimpleFunction,
        retargets: Collection<IrSimpleFunctionSymbol>
    ) {
        for (retarget in retargets) {
            val override = if (retarget.owner.origin != JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT) {
                retarget.owner.overriddenSymbols += methodSymbol
                replacements.getReplacementFunction(retarget.owner) ?: retarget.owner
            } else retarget.owner

            override.overriddenSymbols += staticReplacement.symbol
        }
    }

    private fun IrClass.createBridgeToSealedInlineClassMethodIfNeeded(
        methodSymbol: IrSimpleFunctionSymbol,
        function: IrSimpleFunction,
        retargets: Map<IrClassSymbol, IrSimpleFunctionSymbol>
    ) {
        val allOverridden = retargets
            .flatMap { (_, retarget) -> retarget.owner.allOverridden() }
            .toSet()

        if (retargets.values.any { it.owner.parentAsClass.isSealedInline } || allOverridden.any { it.parentAsClass.isInterface }) {
            val bridge = context.irFactory.buildFun {
                updateFrom(methodSymbol.owner)
                origin = methodSymbol.owner.origin
                modality = Modality.OPEN
                name = methodSymbol.owner.name
                returnType = methodSymbol.owner.returnType
            }.apply {
                copyParameterDeclarationsFrom(methodSymbol.owner)
                annotations = methodSymbol.owner.annotations
                parent = this@createBridgeToSealedInlineClassMethodIfNeeded
            }
            addMember(bridge)

            val returnBoxed = bridge.returnType.isInlineClassType() && !bridge.returnType.isNullable() &&
                    allOverridden.any {
                        it.returnType.isNullable() || it.returnType.isAny() ||
                                it.returnType.isNullableAny() || it.returnType.isTypeParameter()
                    }

            createBridgeBody(bridge, function, returnBoxed)
        }
    }

    // Remove fake overrides of default methods, we generated its replacement ourselves
    private fun IrClass.removeFakeOverrideOfDefaultMethodFromSealedInlineClass(methodSymbol: IrSimpleFunctionSymbol) {
        if (methodSymbol.owner.origin == JvmLoweredDeclarationOrigin.GENERATED_SEALED_INLINE_CLASS_METHOD) {
            val fakeOverride = findFakeOverrideOfInterfaceMethodInSealedInlineClass(methodSymbol)
            if (fakeOverride != null && fakeOverride.allOverridden().any {
                    it.parentAsClass.isInterface && it.modality != Modality.ABSTRACT
                }
            ) {
                declarations.remove(fakeOverride)
            }
        }
    }

    private fun IrClass.findFakeOverrideOfInterfaceMethodInSealedInlineClass(methodSymbol: IrSimpleFunctionSymbol): IrSimpleFunction? =
        functions.find {
            it.isFakeOverride && context.inlineClassReplacements
                .getSealedInlineClassChildFunctionInTop(this to it.withoutReceiver()).symbol == methodSymbol
        }

    private fun IrBuilderWithScope.callDefaultMethodOfSealedInlineClassSuperinterface(
        top: IrClass,
        methodSymbol: IrSimpleFunctionSymbol,
        function: IrSimpleFunction
    ): IrExpression? {
        if (methodSymbol.owner.origin == JvmLoweredDeclarationOrigin.GENERATED_SEALED_INLINE_CLASS_METHOD) {
            val fakeOverride = top.findFakeOverrideOfInterfaceMethodInSealedInlineClass(methodSymbol)
            if (fakeOverride != null) {
                val defaultMethod = fakeOverride.overriddenSymbols.find {
                    it.owner.parentAsClass.isInterface && it.owner.modality != Modality.ABSTRACT
                }
                if (defaultMethod != null) {
                    val callDefaultImpls = this@JvmInlineClassLowering.context.state.jvmDefaultMode
                        .let { it.isEnabled && !it.forAllMethodsWithBody }
                    if (callDefaultImpls) {
                        val defaultImplsMethod = this@JvmInlineClassLowering.context.cachedDeclarations
                            .getDefaultImplsFunction(defaultMethod.owner)
                        return irCall(defaultImplsMethod.symbol).apply {
                            for ((target, source) in defaultImplsMethod.explicitParameters.zip(function.explicitParameters)) {
                                putArgument(target, irGet(source))
                            }
                        }
                    } else {
                        return irCall(defaultMethod.owner, superQualifierSymbol = defaultMethod.owner.parentAsClass.symbol).apply {
                            dispatchReceiver = irImplicitCast(
                                irGet(function.valueParameters[0]),
                                defaultMethod.owner.parentAsClass.defaultType
                            )
                            for ((target, source) in defaultMethod.owner.explicitParameters.zip(function.explicitParameters).drop(1)) {
                                putArgument(target, irGet(source))
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    /* The equals function for sealed inline classes looks like
     * (given the class is named Top)
     *
     * fun equals(other: Any?) =
     *   other is Top && equal-impl0(this.$value, other.$value)
     */
    private fun generateEqualsMethodForSealedInlineClass(top: IrClass, function: IrSimpleFunction) {
        val equals = context.inlineClassReplacements.getSpecializedEqualsMethod(top, context.irBuiltIns)

        with(context.createIrBuilder(function.symbol)) {
            function.body = irExprBody(irAndand(
                irIs(irGet(function.valueParameters[1]), top.defaultType),
                irCall(equals).apply {
                    putValueArgument(0, coerceFromSealedInlineClass(top, irGet(function.valueParameters[0])))
                    putValueArgument(1, coerceFromSealedInlineClass(top, irGet(function.valueParameters[1])))
                }
            ))
        }

        // Replace fake override with generated function
        val fakeOverride = top.functions.single {
            it.origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT &&
                    it.name.asString() == "equals-impl" && it.valueParameters.size == 2 &&
                    it.valueParameters[0].type.classOrNull == top.symbol &&
                    it.valueParameters[1].type == context.irBuiltIns.anyNType
        }
        top.declarations.remove(fakeOverride)
        top.addMember(function)
    }

    private fun coerceFromSealedInlineClass(top: IrClass, expr: IrExpression): IrExpression =
        coerceInlineClasses(expr, top.defaultType, context.irBuiltIns.anyNType)

    private fun coerceToSealedInlineClass(expr: IrExpression, top: IrClass): IrExpression =
        coerceInlineClasses(expr, context.irBuiltIns.anyNType, top.defaultType)

    private fun buildUnboxFunction(irClass: IrClass) {
        val function = context.inlineClassReplacements.getUnboxFunction(irClass)
        val field = getInlineClassBackingField(irClass)

        function.body = with(context.createIrBuilder(function.symbol)) {
            irExprBody(irGetField(irGet(function.dispatchReceiverParameter!!), field))
        }

        irClass.declarations += function
    }

    private fun buildSpecializedEqualsMethodIfNeeded(valueClass: IrClass) {
        val function = context.inlineClassReplacements.getSpecializedEqualsMethod(valueClass, context.irBuiltIns)
        // Return if we have already built specialized equals as static replacement of typed equals
        if (function.body != null) return
        val left = function.valueParameters[0]
        val right = function.valueParameters[1]
        val type = left.type.unboxInlineClass()

        val untypedEquals = valueClass.functions.single { it.isEquals() }

        function.body = context.createIrBuilder(valueClass.symbol).run {
            val context = this@JvmInlineClassLowering.context
            val underlyingType = getInlineClassUnderlyingType(valueClass)
            irExprBody(
                if (untypedEquals.origin == IrDeclarationOrigin.DEFINED) {
                    val boxFunction = context.inlineClassReplacements.getBoxFunction(valueClass)

                    fun irBox(expr: IrExpression) = irCall(boxFunction).apply { putValueArgument(0, expr) }

                    irCall(untypedEquals).apply {
                        dispatchReceiver = irBox(coerceInlineClasses(irGet(left), left.type, underlyingType))
                        putValueArgument(0, irBox(coerceInlineClasses(irGet(right), right.type, underlyingType)))
                    }
                } else {
                    val underlyingClass = underlyingType.getClass()
                    // We can't directly compare unboxed values of underlying inline class as this class can have custom equals
                    if (underlyingClass?.isInline == true && !underlyingType.isNullable()) {
                        val underlyingClassEq =
                            context.inlineClassReplacements.getSpecializedEqualsMethod(underlyingClass, context.irBuiltIns)
                        irCall(underlyingClassEq).apply {
                            putValueArgument(0, coerceInlineClasses(irGet(left), left.type, underlyingType))
                            putValueArgument(1, coerceInlineClasses(irGet(right), right.type, underlyingType))
                        }
                    } else {
                        irEquals(coerceInlineClasses(irGet(left), left.type, type), coerceInlineClasses(irGet(right), right.type, type))
                    }
                }
            )
        }

        valueClass.declarations += function
    }

    /* For sealed inline classes, inside `equals-impl0` we check for types of parameters and then call respective `equals` of `equals-impl0`
     * functions.
     *
     * For example, given the code
     *
     * @JvmInline
     * sealed value class Top {
     *   @JvmInline
     *   value class I(val i: Int)
     *   @JvmInline
     *   value class L(val l: Long)
     *
     *   value class S(...)
     * }
     *
     * First, we generate checks for noinline subclasses, and then for inline ones.
     * For noinline classes we just generate reference equals, since we do not support user-defined equals for value classes yet.
     *
     * So, we generate the following function
     *
     * static fun Top.equals-impl0($this: Any?, other: Any?): Boolean = when {
     *   $this is S -> $this === other
     *   $this is Int && other is Int -> I.equals-impl0($this as Int?, other as Int?)
     *   $this is Long && other is Long -> L.equals-impl0($this as Long?, other as Long?)
     *   else -> false
     * }
     */
    private fun buildSpecializedEqualsMethodForSealed(info: SealedInlineClassInfo) {
        val irClass = info.top
        val inlineSubclasses = info.inlineSubclasses
        val noinlineSubclasses = info.noinlineSubclasses

        val function = context.inlineClassReplacements.getSpecializedEqualsMethod(irClass, context.irBuiltIns)

        with(context.createIrBuilder(function.symbol)) {
            function.body = irBlockBody {
                val left = irTemporary(
                    coerceToSealedInlineClass(irGet(function.valueParameters[0]), irClass)
                )

                val right = irTemporary(
                    coerceToSealedInlineClass(irGet(function.valueParameters[1]), irClass)
                )

                val branches = mutableListOf<IrBranch>()

                for (noinlineSubclass in noinlineSubclasses) {
                    branches += irBranch(
                        irIs(irGet(function.valueParameters[0]), noinlineSubclass.owner.defaultType),
                        irEqeqeq(irGet(left), irGet(right))
                    )
                }

                for (inlineSubclass in inlineSubclasses) {
                    val eq = this@JvmInlineClassLowering.context.inlineClassReplacements
                        .getSpecializedEqualsMethod(inlineSubclass.owner, context.irBuiltIns)
                    val underlyingType = context.irBuiltIns.getInlineClassUnderlyingType(inlineSubclass.owner)
                    branches += irBranch(
                        irAndand(
                            irIs(irGet(function.valueParameters[0]), underlyingType),
                            irIs(irGet(function.valueParameters[1]), underlyingType)
                        ),
                        irCall(eq).apply {
                            putValueArgument(0, irGet(left))
                            putValueArgument(1, irGet(right))
                        }
                    )
                }

                branches += irElseBranch(irFalse())

                +irReturn(irWhen(context.irBuiltIns.booleanType, branches))
            }
        }

        irClass.declarations += function
    }
}

internal fun IrClass.isChildOfSealedInlineClass(): Boolean = superTypes.any { it.isInlineClassType() }

private class SealedInlineClassInfo(
    val top: IrClass,
    val inlineSubclasses: Set<IrClassSymbol>,
    val noinlineSubclasses: Set<IrClassSymbol>,
    val sealedInlineSubclasses: List<IrClassSymbol>,
    val methods: Set<MethodInfo>,
) {
    /*
     * If child of sealed inline class fake overrides a method, we need to 'retarget' the call inside a giant switch-case in top
     * See [rewriteOpenMethodsForSealed]
     *
     * [retargets] is a map from child class to the method, which should be called.
     */
    data class MethodInfo(
        val symbol: IrSimpleFunctionSymbol,
        val retargets: Map<IrClassSymbol, IrSimpleFunctionSymbol>,
        val addToClass: Boolean
    )

    companion object {
        fun analyze(top: IrClass, context: JvmBackendContext): SealedInlineClassInfo {
            fun analyzeHierarchy(irClass: IrClass, predicate: (IrClassSymbol) -> Boolean): Set<IrClassSymbol> =
                irClass.sealedSubclasses.flatMap {
                    when {
                        it.owner.isSealedInline -> analyzeHierarchy(it.owner, predicate)
                        predicate(it) -> setOf(it)
                        else -> emptySet()
                    }
                }.toSet()

            val sealedInlineSubclasses = mutableListOf<IrClassSymbol>()

            var cursor: IrClassSymbol? = top.symbol
            while (cursor != null) {
                sealedInlineSubclasses += cursor
                cursor = cursor.owner.sealedSubclasses.find { it.owner.isSealedInline }
            }

            return SealedInlineClassInfo(
                top,
                analyzeHierarchy(top) { it.owner.isInline },
                analyzeHierarchy(top) { !it.owner.isInline },
                sealedInlineSubclasses.reversed(),
                analyzeMethods(top, context)
            )
        }

        private fun analyzeMethods(top: IrClass, context: JvmBackendContext): Set<MethodInfo> {
            val res = mutableSetOf<MethodInfo>()
            val visited = mutableSetOf<IrSimpleFunctionSymbol>()
            // Methods, declared in top
            for (method in top.functions) {
                if (method.origin != IrDeclarationOrigin.DEFINED) continue
                visited += method.symbol
                val retargets = mutableMapOf<IrClassSymbol, IrSimpleFunctionSymbol>()
                colorChildren(top, method.symbol, method.symbol, retargets, visited)
                res += MethodInfo(method.symbol, retargets, addToClass = method.modality == Modality.ABSTRACT)
            }

            // Methods, declared in children
            val retargetsInTop = mutableMapOf<IrSimpleFunction, MutableMap<IrClassSymbol, IrSimpleFunctionSymbol>>()
            var currentClass = top
            var doContinue = true
            while (doContinue) {
                doContinue = false
                for (subclass in currentClass.sealedSubclasses) {
                    if (subclass.owner.isSealedInline) {
                        currentClass = subclass.owner
                        doContinue = true
                    }

                    for (method in subclass.functions) {
                        if (method.owner.origin != IrDeclarationOrigin.DEFINED) continue
                        if (!visited.add(method)) continue

                        val retargets = mutableMapOf<IrClassSymbol, IrSimpleFunctionSymbol>()
                        colorChildren(subclass.owner, method, method, retargets, visited)
                        if (method.owner.modality != Modality.ABSTRACT) {
                            retargets[subclass] = method
                        }

                        val methodInTop =
                            context.inlineClassReplacements.getSealedInlineClassChildFunctionInTop(top to method.owner.withoutReceiver())
                        retargetsInTop.getOrPut(methodInTop) { mutableMapOf() }.putAll(retargets)
                    }
                }
            }

            for ((methodInTop, retargets) in retargetsInTop) {
                res += MethodInfo(methodInTop.symbol, retargets, addToClass = true)
            }

            return res
        }

        /*
         * Go down through hierarchy of sealed inline classes and find retargets of fake overrides
         * See [MethodInfo]
         */
        private fun colorChildren(
            irClass: IrClass,
            target: IrSimpleFunctionSymbol,
            method: IrSimpleFunctionSymbol,
            retargets: MutableMap<IrClassSymbol, IrSimpleFunctionSymbol>,
            visited: MutableSet<IrSimpleFunctionSymbol>
        ) {
            for (child in irClass.sealedSubclasses) {
                val override = child.functions.find { function ->
                    method.owner.attributeOwnerId in function.owner.overriddenSymbols.map { it.owner.attributeOwnerId }
                } ?: continue
                visited += override
                if (override.owner.isFakeOverride) {
                    retargets[child] = target
                    colorChildren(child.owner, target, override, retargets, visited)
                } else {
                    retargets[child] = override
                    colorChildren(child.owner, override, override, retargets, visited)
                }
            }
        }
    }
}

private fun IrSimpleFunction.isFakeOverrideOfDefaultMethod(): Boolean =
    isFakeOverride && overriddenSymbols.any { it.owner.parentAsClass.isInterface && it.owner.modality != Modality.ABSTRACT }
