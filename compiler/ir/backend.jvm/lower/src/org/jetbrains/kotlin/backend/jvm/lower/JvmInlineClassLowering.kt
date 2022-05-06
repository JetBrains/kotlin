/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.common.lower.loops.forLoopsPhase
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
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
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

val jvmInlineClassPhase = makeIrFilePhase(
    ::JvmInlineClassLowering,
    name = "Inline Classes",
    description = "Lower inline classes",
    // forLoopsPhase may produce UInt and ULong which are inline classes.
    // Standard library replacements are done on the unmangled names for UInt and ULong classes.
    // Collection stubs may require mangling by inline class rules.
    // SAM wrappers may require mangling for fun interfaces with inline class parameters
    prerequisite = setOf(forLoopsPhase, jvmBuiltInsPhase, collectionStubMethodLowering, singleAbstractMethodPhase),
)

/**
 * Adds new constructors, box, and unbox functions to inline classes as well as replacement
 * functions and bridges to avoid clashes between overloaded function. Changes calls with
 * known types to call the replacement functions.
 *
 * We do not unfold inline class types here. Instead, the type mapper will lower inline class
 * types to the types of their underlying field.
 */
private class JvmInlineClassLowering(context: JvmBackendContext) : JvmValueClassAbstractLowering(context) {
    override val replacements: MemoizedValueClassAbstractReplacements
        get() = context.inlineClassReplacements

    override fun IrClass.isSpecificLoweringLogicApplicable(): Boolean = isSingleFieldValueClass

    override fun IrFunction.isSpecificFieldGetter(): Boolean = isInlineClassFieldGetter

    override fun buildAdditionalMethodsForSealedInlineClass(declaration: IrClass, constructor: IrConstructor) {
        if (declaration.isChildOfSealedInlineClass() && declaration.isInline) {
            if (declaration.modality != Modality.SEALED) {
                updateGetterForSealedInlineClassChild(declaration, declaration.defaultType.findTopSealedInlineSuperClass())
                buildSpecializedEqualsMethod(declaration)
            }
            rewriteConstructorForSealedInlineClassChild(declaration, declaration.sealedInlineClassParent(), constructor)
            removeMethods(declaration)
        }

        if (declaration.modality == Modality.SEALED && !declaration.isChildOfSealedInlineClass()) {
            buildCommonAdditionalMethods(declaration, constructor)

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
        if ((currentClass?.irElement as? IrClass)?.isInline == true) {
            return super.visitTypeOperator(expression)
        }

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
                                irCall(isCheck.symbol).also {
                                    it.putValueArgument(
                                        0,
                                        coerceInlineClasses(irGet(tmp), top.defaultType, context.irBuiltIns.anyNType)
                                    )
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
                                irNot(
                                    irCall(isCheck.symbol).also {
                                        it.putValueArgument(
                                            0,
                                            coerceInlineClasses(irGet(tmp), top.defaultType, context.irBuiltIns.anyNType)
                                        )
                                    }
                                )
                            )
                        }
                    }
                }
                IrTypeOperator.CAST -> {
                    // if (Top.is-Child(top)) top else CCE
                    return generateAsCheck(currentScopeSymbol, transformed, top, isCheck, underlyingType) {
                        irCall(this@JvmInlineClassLowering.context.ir.symbols.throwTypeCastException).also {
                            it.putValueArgument(
                                0,
                                irString("Cannot cast to sealed inline class child ${expression.typeOperand.classFqName}")
                            )
                        }
                    }
                }
                IrTypeOperator.SAFE_CAST -> {
                    // if (Top.is-Child(top)) top else null
                    return generateAsCheck(currentScopeSymbol, transformed, top, isCheck, underlyingType) { irNull() }
                }
                else -> {
                    return transformed
                }
            }
        }
        return super.visitTypeOperator(expression)
    }

    private fun generateAsCheck(
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
                        val unboxedTmp = irTemporary(coerceInlineClasses(irGet(tmp), top.defaultType, context.irBuiltIns.anyNType))
                        +irWhen(
                            expression.type, listOf(
                                irBranch(
                                    irCall(isCheck.symbol).also { it.putValueArgument(0, irGet(unboxedTmp)) },
                                    coerceInlineClasses(
                                        irImplicitCast(irGet(unboxedTmp), underlyingType),
                                        underlyingType,
                                        expression.typeOperand
                                    )
                                ),
                                irBranch(irTrue(), onFail())
                            )
                        )
                    }
                )
            }
        }
    }

    private fun buildIsMethodsForSealedInlineClass(info: SealedInlineClassInfo) {
        for (childInfo in info.inlineSubclasses + info.sealedInlineSubclasses.dropLast(1)) {
            val child = childInfo.owner
            val function = context.inlineClassReplacements.getIsSealedInlineChildFunction(info.top to child)
            val underlyingType = child.inlineClassRepresentation!!.underlyingType

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
                            irIs(
                                coerceInlineClasses(param(), context.irBuiltIns.anyNType, info.top.defaultType),
                                noinline.owner.defaultType
                            ),
                            irReturn(irFalse())
                        )
                    }

                    branches += irBranch(
                        irTrue(),
                        irReturn(irIs(param(), underlyingType))
                    )

                    +irReturn(irWhen(context.irBuiltIns.booleanType, branches))
                }
            }

            info.top.addMember(function)
        }
    }

    // Since we cannot create objects of sealed inline class children, we remove virtual methods from the classfile.
    private fun removeMethods(irClass: IrClass) {
        irClass.declarations.removeIf {
            it is IrSimpleFunction &&
                    (it.origin == IrDeclarationOrigin.GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER ||
                            it.origin == IrDeclarationOrigin.DEFINED ||
                            it.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR)
        }
    }

    private fun rewriteConstructorForSealedInlineClassChild(irClass: IrClass, parent: IrClass, irConstructor: IrConstructor) {
        val toCall = context.inlineClassReplacements.getReplacementFunction(
            parent.primaryConstructor
                ?: parent.constructors.single { it.origin == JvmLoweredDeclarationOrigin.PRIMARY_CONSTRUCTOR_FOR_SEALED_INLINE_CLASS }
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

        val methodToOverride = top.functions.single { it.name == InlineClassAbi.sealedInlineClassFieldName }

        require(
            methodToOverride.origin == IrDeclarationOrigin.FAKE_OVERRIDE ||
                    methodToOverride.origin == IrDeclarationOrigin.GETTER_OF_SEALED_INLINE_CLASS_FIELD
        )

        val fakeOverride = IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrDeclarationOrigin.FAKE_OVERRIDE,
            IrSimpleFunctionSymbolImpl(),
            methodToOverride.name,
            methodToOverride.visibility,
            methodToOverride.modality,
            methodToOverride.returnType,
            methodToOverride.isInline,
            methodToOverride.isExternal,
            methodToOverride.isTailrec,
            methodToOverride.isSuspend,
            methodToOverride.isOperator,
            methodToOverride.isInfix,
            methodToOverride.isExpect
        ).also {
            it.parent = irClass
        }

        fakeOverride.overriddenSymbols += methodToOverride.symbol
        fakeOverride.dispatchReceiverParameter = irClass.thisReceiver?.copyTo(fakeOverride, type = irClass.defaultType)

        irClass.addMember(fakeOverride)

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

    override fun transformSimpleFunctionFlat(function: IrSimpleFunction, replacement: IrSimpleFunction): List<IrDeclaration> {
        if (function.modality == Modality.ABSTRACT) return emptyList()

        if (function.parentAsClass.modality == Modality.SEALED && function.isFakeOverride) return listOf(function)

        replacement.valueParameters.forEach {
            it.transformChildrenVoid()
            it.defaultValue?.patchDeclarationParents(replacement)
        }
        allScopes.push(createScope(function))
        replacement.body = function.body?.transform(this, null)?.patchDeclarationParents(replacement)
        allScopes.pop()
        replacement.copyAttributes(function)

        // Don't create a wrapper for functions which are only used in an unboxed context
        // However, sealed inline classes do have overrides.
        if ((function.overriddenSymbols.isEmpty() || replacement.dispatchReceiverParameter != null) &&
            function.parentAsClass.modality != Modality.SEALED
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
            overridden.owner.parent.let { it is IrClass && it.isInline }
        }

        // Replace the function body with a wrapper
        if (!bridgeFunction.isFakeOverride || !bridgeFunction.parentAsClass.isInline) {
            createBridgeBody(bridgeFunction, replacement)
        } else if (overriddenFromSealedInline != null) {
            // If fake override overrides function from sealed inline class, call the overridden function
            createBridgeBody(replacement, overriddenFromSealedInline.owner)
            // However, if the fake override is overridden, generate when in it, calling its children.
            // TODO: Rewrite SIC methods
        } else {
            // Fake overrides redirect from the replacement to the original function, which is in turn replaced during interfacePhase.
            createBridgeBody(replacement, bridgeFunction)
        }

        return listOf(replacement, bridgeFunction)
    }

    private fun IrSimpleFunction.signatureRequiresMangling() =
        fullValueParameterList.any { it.type.requiresMangling } ||
                context.state.functionsWithInlineClassReturnTypesMangled && returnType.requiresMangling

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

    private fun createBridgeBody(source: IrSimpleFunction, target: IrSimpleFunction) {
        source.body = context.createIrBuilder(source.symbol, source.startOffset, source.endOffset).run {
            irExprBody(irCall(target).apply {
                passTypeArgumentsFrom(source)
                for ((parameter, newParameter) in source.explicitParameters.zip(target.explicitParameters)) {
                    putArgument(newParameter, irGet(parameter))
                }
            })
        }
    }

    // Secondary constructors for boxed types get translated to static functions returning
    // unboxed arguments. We remove the original constructor.
    // Primary constructors' case is handled at the start of transformFunctionFlat
    override fun transformConstructorFlat(constructor: IrConstructor, replacement: IrSimpleFunction): List<IrDeclaration> {
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

    private fun typedArgumentList(function: IrFunction, expression: IrMemberAccessExpression<*>) =
        listOfNotNull(
            function.dispatchReceiverParameter?.let { it to expression.dispatchReceiver },
            function.extensionReceiverParameter?.let { it to expression.extensionReceiver }
        ) + function.valueParameters.map { it to expression.getValueArgument(it.index) }

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
            expression is IrDelegatingConstructorCall && function.parentAsClass.isInline &&
                    (currentClass?.irElement as? IrClass)?.isInline == false

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

        val inlineClass = (function!!.parent as? IrClass)?.takeIf { it.isInline } ?: return function

        val topSealedInlineClass =
            if (inlineClass.isChildOfSealedInlineClass()) inlineClass.defaultType.findTopSealedInlineSuperClass()
            else null

        if (topSealedInlineClass != null) {
            while (function != null && function.parent != topSealedInlineClass) {
                function = function.overriddenSymbols.find { it.owner.parentAsClass.isInline }?.owner
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
        val leftIsUnboxed = left.type.unboxInlineClass() != left.type
        val rightIsUnboxed = right.type.unboxInlineClass() != right.type
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
        return if (leftNullCheck || rightNullCheck) {
            irBlock {
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
        } else {
            equals(left, right)
        }
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
            expression.isSpecializedInlineClassEqEq -> {
                expression.transformChildrenVoid()
                context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                    .specializeEqualsCall(expression.getValueArgument(0)!!, expression.getValueArgument(1)!!)
                    ?: expression
            }
            else ->
                super.visitCall(expression)
        }

    private val IrCall.isSpecializedInlineClassEqEq: Boolean
        get() {
            // Note that reference equality (x === y) is not allowed on values of inline class type,
            // so it is enough to check for eqeq.
            if (symbol != context.irBuiltIns.eqeqSymbol)
                return false

            val leftClass = getValueArgument(0)?.type?.classOrNull?.owner?.takeIf { it.isInline }
                ?: return false

            // Before version 1.4, we cannot rely on the Result.equals-impl0 method
            return (leftClass.fqNameWhenAvailable != StandardNames.RESULT_FQ_NAME) ||
                    context.state.languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4
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

    override fun buildPrimaryValueClassConstructor(valueClass: IrClass, irConstructor: IrConstructor) {
        // Add the default primary constructor
        valueClass.addConstructor {
            updateFrom(irConstructor)
            visibility = if (valueClass.modality == Modality.SEALED) DescriptorVisibilities.PROTECTED else DescriptorVisibilities.PRIVATE
            origin =
                if (valueClass.modality == Modality.SEALED) JvmLoweredDeclarationOrigin.PRIMARY_CONSTRUCTOR_FOR_SEALED_INLINE_CLASS
                else JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER
            returnType = irConstructor.returnType
        }.apply {
            if (valueClass.modality == Modality.SEALED) {
                valueParameters = listOf(
                    context.irFactory.createValueParameter(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        JvmLoweredDeclarationOrigin.PRIMARY_CONSTRUCTOR_PARAMETER_FOR_SEALED_INLINE_CLASS,
                        IrValueParameterSymbolImpl(),
                        InlineClassAbi.sealedInlineClassFieldName,
                        index = 0,
                        type = context.irBuiltIns.anyNType,
                        varargElementType = null,
                        isCrossinline = false,
                        isNoinline = false,
                        isHidden = false,
                        isAssignable = false
                    ).also {
                        it.parent = this
                    }
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

    override fun buildBoxFunction(valueClass: IrClass) {
        val function = context.inlineClassReplacements.getBoxFunction(valueClass)

        val primaryConstructor =
            if (valueClass.modality == Modality.SEALED) {
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

    override fun buildUnboxFunctions(valueClass: IrClass) {
        buildUnboxFunction(valueClass)
    }

    private fun rewriteMethodsForSealed(info: SealedInlineClassInfo) {
        rewriteOpenMethodsForSealed(info)
    }

    /**
     * Methods of sealed inline classes and their children all go through the top,
     * where we check, which one we should call in giant switch-case.
     *
     * First, we check for noinline children, then for inline children and finally, just run the top's method body.
     */
    private fun rewriteOpenMethodsForSealed(info: SealedInlineClassInfo) {
        for ((methodSymbol, retargets, addToClass) in info.methods) {
            val replacements = context.inlineClassReplacements
            val original = methodSymbol.owner.attributeOwnerId as IrSimpleFunction
            val function = replacements.getReplacementFunction(original)
                ?: replacements.getReplacementFunction(
                    replacements.getSealedInlineClassChildFunctionInTop(info.top to original.withoutReceiver())
                ) ?: error("Cannot find replacement for ${methodSymbol.owner.render()}")
            val oldBody = function.body

            fun findFakeOverrideOfInterfaceMethod(): IrSimpleFunction? =
                info.top.functions.find {
                    it.name == methodSymbol.owner.name && it.isFakeOverride &&
                            replacements.getSealedInlineClassChildFunctionInTop(info.top to it.withoutReceiver()).symbol == methodSymbol
                }

            with(context.createIrBuilder(function.symbol)) {
                function.body = irBlockBody {
                    var counter = 0
                    fun copyOldBody() = when (oldBody) {
                        is IrExpressionBody -> irReturn(oldBody.expression.deepCopySavingMetadata())
                        is IrBlockBody -> irBlock {
                            for (stmt in oldBody.statements) {
                                +stmt.deepCopySavingMetadata()
                            }
                        }
                        null -> {
                            var expression: IrExpression? = null
                            if (methodSymbol.owner.origin == JvmLoweredDeclarationOrigin.GENERATED_SEALED_INLINE_CLASS_METHOD) {
                                val fakeOverride = findFakeOverrideOfInterfaceMethod()
                                if (fakeOverride != null) {
                                    val defaultMethod = fakeOverride.overriddenSymbols.find {
                                        it.owner.parentAsClass.isInterface && it.owner.modality != Modality.ABSTRACT
                                    }
                                    if (defaultMethod != null) {
                                        val defaultImplsMethod =
                                            this@JvmInlineClassLowering.context.cachedDeclarations
                                                .getDefaultImplsFunction(defaultMethod.owner)
                                        expression = irCall(defaultImplsMethod.symbol).also {
                                            for ((target, source) in defaultImplsMethod.explicitParameters
                                                .zip(function.explicitParameters)
                                            ) {
                                                it.putArgument(target, irGet(source))
                                            }
                                        }
                                    }
                                }
                            }
                            expression ?: irThrow(
                                irCall(this@JvmInlineClassLowering.context.ir.symbols.illegalStateExceptionCtorString).also {
                                    it.putValueArgument(0, irString("${counter++}"))
                                }
                            )
                        }
                        else -> error("oldBody is not a function body: ${oldBody.dump()}")
                    }

                    val receiver = irTemporary(
                        coerceInlineClasses(irGet(function.valueParameters[0]), info.top.defaultType, context.irBuiltIns.anyNType),
                        "anyN_receiver"
                    )

                    val branches = mutableListOf<IrBranch>()

                    for (noinlineSubclass in info.noinlineSubclasses) {
                        val retarget = retargets[noinlineSubclass] ?: continue
                        val retargetClass = retarget.owner.parentAsClass
                        val toCall =
                            if (retargetClass.isInline) replacements.getReplacementFunction(retarget.owner)!!.symbol else retarget

                        branches +=
                            irBranch(
                                irIs(irGet(function.valueParameters[0]), noinlineSubclass.owner.defaultType),
                                if (retargetClass.symbol == info.top.symbol) copyOldBody()
                                else irCall(toCall).apply {
                                    if (retargetClass.isInline) {
                                        putValueArgument(0, irGet(function.valueParameters[0]))
                                    } else {
                                        dispatchReceiver =
                                            irImplicitCast(irGet(function.valueParameters[0]), noinlineSubclass.owner.defaultType)
                                    }
                                    for ((target, source) in toCall.owner.explicitParameters.zip(function.explicitParameters).drop(1)) {
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
                                if (retargetClass.symbol == info.top.symbol) copyOldBody()
                                else irReturn(irCall(toCall).apply {
                                    putValueArgument(
                                        0, coerceInlineClasses(
                                            irImplicitCast(irGet(receiver), underlyingType),
                                            underlyingType,
                                            inlineSubclass.owner.defaultType
                                        )
                                    )
                                    for ((target, source) in toCall.owner.explicitParameters.zip(function.explicitParameters).drop(1)) {
                                        putArgument(target, irGet(source))
                                    }
                                })
                            )
                    }

                    val sealedInlineSubclass = info.sealedInlineSubclasses.first()
                    var retarget = retargets[sealedInlineSubclass]
                    if (retarget != null && retarget.owner.origin != JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT) {
                        retarget = replacements.getReplacementFunction(retarget.owner)?.symbol
                    }

                    if (retarget != null) {
                        branches += irBranch(
                            irTrue(),
                            if (retarget.owner.parentAsClass.symbol == info.top.symbol) copyOldBody()
                            else irReturn(irCall(retarget).apply {
                                putValueArgument(
                                    0, coerceInlineClasses(
                                        irImplicitCast(irGet(receiver), context.irBuiltIns.anyNType),
                                        context.irBuiltIns.anyNType,
                                        sealedInlineSubclass.owner.defaultType.findTopSealedInlineSuperClass().defaultType
                                    )
                                )
                                for ((target, source) in retarget.owner.explicitParameters.zip(function.explicitParameters).drop(1)) {
                                    putArgument(target, irGet(source))
                                }
                            })
                        )
                    } else {
                        branches += irBranch(irTrue(), copyOldBody())
                    }

                    +irReturn(irWhen(function.returnType, branches))
                }
            }

            if (addToClass) {
                if (methodSymbol.owner.modality != Modality.ABSTRACT) {
                    info.top.addMember(function)
                }

                if (retargets.any { (_, retarget) ->
                        val retargetToSealedInline = retarget.owner.parentAsClass.let { it.isInline && it.modality == Modality.SEALED }
                        if (retargetToSealedInline) return@any true
                        val retargetToOverriddenInInterface = retarget.owner.allOverridden().any { it.parentAsClass.isInterface }
                        retargetToOverriddenInInterface
                    }
                ) {
                    val bridge = context.irFactory.buildFun {
                        updateFrom(methodSymbol.owner)
                        origin = methodSymbol.owner.origin
                        modality = Modality.OPEN
                        name = methodSymbol.owner.name
                        returnType = methodSymbol.owner.returnType
                    }.apply {
                        copyParameterDeclarationsFrom(methodSymbol.owner)
                        annotations = methodSymbol.owner.annotations
                        parent = info.top
                    }
                    info.top.addMember(bridge)
                    createBridgeBody(bridge, function)
                }

                for ((_, retarget) in retargets) {
                    val override = if (retarget.owner.origin != JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT) {
                        retarget.owner.overriddenSymbols += methodSymbol
                        replacements.getReplacementFunction(retarget.owner) ?: retarget.owner
                    } else retarget.owner

                    override.overriddenSymbols += function.symbol
                }

                // Remove fake overrides of default methods, we generated its replacement ourselves
                if (methodSymbol.owner.origin == JvmLoweredDeclarationOrigin.GENERATED_SEALED_INLINE_CLASS_METHOD) {
                    val fakeOverride = findFakeOverrideOfInterfaceMethod()
                    if (fakeOverride != null && fakeOverride.allOverridden().any {
                            it.parentAsClass.isInterface && it.modality != Modality.ABSTRACT
                    }) {
                        info.top.declarations.remove(fakeOverride)
                    }
                }
            }
        }
    }

    private fun buildUnboxFunction(irClass: IrClass) {
        val function = context.inlineClassReplacements.getUnboxFunction(irClass)
        val field = getInlineClassBackingField(irClass)

        function.body = context.createIrBuilder(function.symbol).irBlockBody {
            val thisVal = irGet(function.dispatchReceiverParameter!!)
            +irReturn(irGetField(thisVal, field))
        }

        irClass.declarations += function
    }

    override fun buildSpecializedEqualsMethod(valueClass: IrClass) {
        val function = context.inlineClassReplacements.getSpecializedEqualsMethod(valueClass, context.irBuiltIns)
        val left = function.valueParameters[0]
        val right = function.valueParameters[1]
        val type = left.type.unboxInlineClass()

        function.body = context.createIrBuilder(valueClass.symbol).run {
            irExprBody(
                irEquals(
                    coerceInlineClasses(irGet(left), left.type, type),
                    coerceInlineClasses(irGet(right), right.type, type)
                )
            )
        }

        valueClass.declarations += function
    }

    private fun buildSpecializedEqualsMethodForSealed(info: SealedInlineClassInfo) {
        val irClass = info.top
        val inlineSubclasses = info.inlineSubclasses
        val noinlineSubclasses = info.noinlineSubclasses

        val boolAnd = context.ir.symbols.getBinaryOperator(
            OperatorNameConventions.AND, context.irBuiltIns.booleanType, context.irBuiltIns.booleanType
        )
        val equals = context.ir.symbols.getBinaryOperator(
            OperatorNameConventions.EQUALS, context.irBuiltIns.anyNType, context.irBuiltIns.anyNType
        )

        val function = context.inlineClassReplacements.getSpecializedEqualsMethod(irClass, context.irBuiltIns)

        with(context.createIrBuilder(function.symbol)) {
            function.body = irBlockBody {
                val left = irTemporary(
                    coerceInlineClasses(
                        irGet(function.valueParameters[0]),
                        function.valueParameters[0].type,
                        context.irBuiltIns.anyType
                    )
                )
                val right = irTemporary(
                    coerceInlineClasses(
                        irGet(function.valueParameters[1]),
                        function.valueParameters[1].type,
                        context.irBuiltIns.anyType
                    )
                )
                val branches = noinlineSubclasses.map {
                    irBranch(
                        irCallOp(
                            boolAnd, context.irBuiltIns.booleanType,
                            irIs(irGet(left), it.owner.defaultType),
                            irIs(irGet(right), it.owner.defaultType),
                        ),
                        irReturn(irCallOp(equals, context.irBuiltIns.booleanType, irGet(left), irGet(right)))
                    )
                } + inlineSubclasses.map {
                    val eq = this@JvmInlineClassLowering.context.inlineClassReplacements
                        .getSpecializedEqualsMethod(it.owner, context.irBuiltIns)
                    val underlyingType = getInlineClassUnderlyingType(it.owner)
                    irBranch(
                        irCallOp(
                            boolAnd, context.irBuiltIns.booleanType,
                            irIs(irGet(left), underlyingType),
                            irIs(irGet(right), underlyingType),
                        ),
                        irReturn(
                            irCall(eq).apply {
                                putValueArgument(
                                    0, coerceInlineClasses(
                                        irImplicitCast(irGet(left), underlyingType),
                                        underlyingType,
                                        it.owner.defaultType
                                    )
                                )
                                putValueArgument(
                                    1, coerceInlineClasses(
                                        irImplicitCast(irGet(right), underlyingType),
                                        underlyingType,
                                        it.owner.defaultType
                                    )
                                )
                            }
                        )
                    )
                } + irBranch(irTrue(), irReturn(irFalse()))
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
                        it.owner.modality == Modality.SEALED -> analyzeHierarchy(it.owner, predicate)
                        predicate(it) -> setOf(it)
                        else -> emptySet()
                    }
                }.toSet()

            val sealedInlineSubclasses = mutableListOf<IrClassSymbol>()

            var cursor: IrClassSymbol? = top.symbol
            while (cursor != null) {
                sealedInlineSubclasses += cursor
                cursor = cursor.owner.sealedSubclasses.find { it.owner.isInline && it.owner.modality == Modality.SEALED }
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
                    if (subclass.owner.modality == Modality.SEALED) {
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

private fun IrSimpleFunction.withoutReceiver() = MemoizedInlineClassReplacements.SimpleFunctionWithoutReceiver(
    name, typeParameters, returnType, extensionReceiverParameter, valueParameters
)