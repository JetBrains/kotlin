/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin.INLINE_CLASS_CONSTRUCTOR_SYNTHETIC_PARAMETER
import org.jetbrains.kotlin.backend.jvm.ir.shouldBeExposedByAnnotationOrFlag
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.JVM_INLINE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.JVM_NAME_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * Adds new constructors, box, and unbox functions to inline classes as well as replacement
 * functions and bridges to avoid clashes between overloaded function. Changes call with
 * known types to call the replacement functions.
 *
 * We do not unfold inline class types here. Instead, the type mapper will lower inline class
 * types to the types of their underlying field.
 */
@PhasePrerequisites(
    // ForLoopsLowering may produce UInt and ULong which are inline classes.
    ForLoopsLowering::class,
    // Standard library replacements are done on the non-mangled names for UInt and ULong classes.
    JvmBuiltInsLowering::class,
    // Collection stubs may require mangling by value class rules.
    CollectionStubMethodLowering::class,
    // SAM wrappers may require mangling for fun interfaces with value class parameters
    JvmSingleAbstractMethodLowering::class,
)
internal class JvmInlineClassLowering(context: JvmBackendContext) : JvmValueClassAbstractLowering(context) {
    override val replacements: MemoizedValueClassAbstractReplacements
        get() = context.inlineClassReplacements

    private val valueMap = mutableMapOf<IrValueSymbol, IrValueDeclaration>()

    override fun addBindingsFor(original: IrFunction, replacement: IrFunction) {
        for ((param, newParam) in original.parameters.zip(replacement.parameters)) {
            valueMap[param.symbol] = newParam
        }
    }

    override fun createBridgeDeclaration(source: IrSimpleFunction, replacement: IrSimpleFunction, mangledName: Name): IrSimpleFunction =
        context.irFactory.buildFun {
            updateFrom(source)
            name = mangledName
        }.apply {
            copyFunctionSignatureFrom(source)
            // Exposed functions should have no @JvmName annotation, since it does not affect them,
            // but always @JvmExposeBoxed, so users can use reflection to get all exposed functions, if they so desire.
            if (source.shouldBeExposedByAnnotationOrFlag(context.config.languageVersionSettings) &&
                source.origin != IrDeclarationOrigin.GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER
            ) {
                annotations = source.annotations.withJvmExposeBoxedAnnotation(source, context).withoutJvmNameAnnotation() +
                        source.copyPropagatedJvmStaticAnnotation()
            } else {
                annotations = source.annotations
            }
            // Non-exposed counterparts should lack @JvmExposeBoxed.
            source.annotations = source.annotations.withoutJvmExposeBoxedAnnotation()

            parent = source.parent
            // We need to ensure that this bridge has the same attribute owner as its static inline class replacement, since this
            // is used in [CoroutineCodegen.isStaticInlineClassReplacementDelegatingCall] to identify the bridge and avoid generating
            // a continuation class.
            copyAttributes(source)
        }

    // If a property is annotated with @JvmStatic, we generate static accessors.
    // Thus, we should expose the accessors. The easiest way to do so is to copy @JvmStatic annotation.
    private fun IrSimpleFunction.copyPropagatedJvmStaticAnnotation(): List<IrConstructorCall> {
        if (!isPropertyAccessor) return emptyList()
        if (hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME)) return emptyList()
        if (!propertyIfAccessor.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME)) return emptyList()
        return propertyIfAccessor.annotations.filter { it.isAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) }.map { it.deepCopyWithSymbols() }
    }

    override fun IrClass.isSpecificLoweringLogicApplicable(): Boolean = isSingleFieldValueClass

    override val specificMangle: SpecificMangle
        get() = SpecificMangle.Inline
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
            handleSpecificNewClass(declaration)
        }

        // Per KEEP, @JvmExposeBoxed annotation is generated only on callables.
        //   As a result, both annotation processors and runtime reflection do not see @JvmExposeBoxed on the containers (file, class)
        //   but rather on each individual operation.
        // So, drop the annotation from classes and files.
        declaration.annotations = declaration.annotations.withoutJvmExposeBoxedAnnotation()

        return declaration
    }

    override fun handleSpecificNewClass(declaration: IrClass) {
        val irConstructor = declaration.primaryConstructor!!
        // The field getter is used by reflection and cannot be removed here unless it is internal.
        declaration.declarations.removeAll {
            it == irConstructor || (it is IrFunction && it.isInlineClassFieldGetter && !it.visibility.isPublicAPI)
        }
        buildPrimaryInlineClassConstructor(declaration, irConstructor)
        buildBoxFunction(declaration)
        buildUnboxFunction(declaration)
        buildSpecializedEqualsMethodIfNeeded(declaration)
        addJvmInlineAnnotation(declaration)
    }

    private fun addJvmInlineAnnotation(valueClass: IrClass) {
        if (valueClass.hasAnnotation(JVM_INLINE_ANNOTATION_FQ_NAME)) return
        val constructor = context.symbols.jvmInlineAnnotation.constructors.first()
        valueClass.annotations = valueClass.annotations + IrAnnotationImpl.fromSymbolOwner(
            constructor.owner.returnType,
            constructor
        )
    }

    override fun createBridgeBody(source: IrSimpleFunction, target: IrSimpleFunction, original: IrFunction, inverted: Boolean) {
        source.body = context.createIrBuilder(source.symbol, source.startOffset, source.endOffset).run {
            irExprBody(irCall(target).apply {
                passTypeArgumentsFrom(source)
                arguments.assignFrom(source.parameters, ::irGet)
            })
        }
    }

    // Secondary constructors for boxed types get translated to static functions returning
    // unboxed arguments. We remove the original constructor.
    // Primary constructors' case is handled at the start of transformFunctionFlat
    override fun transformSecondaryConstructorFlat(constructor: IrConstructor, replacement: IrSimpleFunction): List<IrDeclaration> {
        replacement.parameters.forEach { it.transformChildrenVoid() }
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

    private fun IrMemberAccessExpression<*>.buildReplacement(original: IrMemberAccessExpression<*>) {
        copyTypeArgumentsFrom(original)
        arguments.assignFrom(original.arguments) { it?.transform(this@JvmInlineClassLowering, null) }
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val function = expression.symbol.owner
        val replacement = context.inlineClassReplacements.getReplacementFunction(function)
            ?: return super.visitFunctionReference(expression)

        // In case of callable reference to inline class constructor,
        // type parameters of the replacement include class's type parameters,
        // however, expression does not. Thus, we should not include them either.
        return IrFunctionReferenceImpl(
            expression.startOffset, expression.endOffset, expression.type,
            replacement.symbol, function.typeParameters.size,
            expression.reflectionTarget, expression.origin
        ).apply {
            buildReplacement(expression)
            copyAttributes(expression)
        }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        val function = expression.symbol.owner
        val replacement = context.inlineClassReplacements.getReplacementFunction(function)
            ?: return super.visitFunctionAccess(expression)

        return IrCallImpl(
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = function.returnType.substitute(expression.typeSubstitutionMap),
            symbol = replacement.symbol,
            typeArgumentsCount = replacement.typeParameters.size,
            origin = expression.origin,
            superQualifierSymbol = (expression as? IrCall)?.superQualifierSymbol
        ).apply {
            buildReplacement(expression)
        }
    }

    private fun coerceInlineClasses(argument: IrExpression, from: IrType, to: IrType, skipCast: Boolean = false): IrExpression {
        return IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, to, context.symbols.unsafeCoerceIntrinsic).apply {
            val underlyingType = from.erasedUpperBound.inlineClassRepresentation?.underlyingType
            if (underlyingType?.isTypeParameter() == true && !skipCast) {
                typeArguments[0] = from
                typeArguments[1] = underlyingType
                arguments[0] = IrTypeOperatorCallImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, to, IrTypeOperator.IMPLICIT_CAST, underlyingType, argument
                )
            } else {
                typeArguments[0] = from
                typeArguments[1] = to
                arguments[0] = argument
            }
        }
    }

    override fun IrConstructor.withAddedMarkerParameterToNonExposedConstructor(): IrConstructor {
        addValueParameter {
            name = Name.identifier("\$boxingMarker")
            origin = JvmLoweredDeclarationOrigin.NON_EXPOSED_CONSTRUCTOR_SYNTHETIC_PARAMETER
            type = context.symbols.boxingConstructorMarkerClass.defaultType.makeNullable()
        }
        return this
    }

    override fun createExposedConstructor(constructor: IrConstructor): IrConstructor =
        constructor.parentAsClass.factory.buildConstructor {
            updateFrom(constructor)
            isPrimary = false
        }.apply {
            copyFunctionSignatureFrom(constructor)
            parameters.forEach { it.defaultValue = null }
            val addedSyntheticParameter =
                parameters.last().origin == JvmLoweredDeclarationOrigin.NON_EXPOSED_CONSTRUCTOR_SYNTHETIC_PARAMETER
            if (addedSyntheticParameter) {
                parameters = parameters.dropLast(1)
            }
            // Only exposed declarations should be annotated with @JvmExposeBoxed in bytecode
            annotations = constructor.annotations.withJvmExposeBoxedAnnotation(constructor, context)
            constructor.annotations = constructor.annotations.withoutJvmExposeBoxedAnnotation()
            body = context.createIrBuilder(this.symbol).irBlockBody(this) {
                +irDelegatingConstructorCall(constructor).apply {
                    for ((index, param) in parameters.withIndex()) {
                        arguments[index] = irGet(param)
                    }
                    if (addedSyntheticParameter) {
                        arguments[parameters.size] = irNull()
                    }
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

            val leftOperandClass = left.type.classOrNull!!.owner
            val equalsMethod = if (rightIsUnboxed && leftOperandClass == right.type.classOrNull!!.owner) {
                this@JvmInlineClassLowering.context.inlineClassReplacements.getSpecializedEqualsMethod(leftOperandClass, context.irBuiltIns)
            } else {
                val equals = leftOperandClass.functions.single { it.name.asString() == "equals" && it.overriddenSymbols.isNotEmpty() }
                this@JvmInlineClassLowering.context.inlineClassReplacements.getReplacementFunction(equals)!!
            }

            return irCall(equalsMethod).apply {
                arguments.assignFrom(listOf(left, right))
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
                val from = expression.symbol.owner.dispatchReceiverParameter!!.type
                val to = context.inlineClassReplacements.getUnboxFunction(from.erasedUpperBound).returnType
                // We need direct unboxed parameter type here
                coerceInlineClasses(arg, from, to)
            }
            // Specialize calls to equals when the left argument is a value of inline class type.
            expression.isEqEqCallOnInlineClass || expression.isEqualsMethodCallOnInlineClass -> {
                expression.transformChildrenVoid()
                context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                    .specializeEqualsCall(left = expression.arguments[0]!!, right = expression.arguments[1]!!)
                    ?: expression
            }
            else ->
                super.visitCall(expression)
        }

    private val IrCall.isEqualsMethodCallOnInlineClass: Boolean
        get() {
            if (!symbol.owner.isEquals()) return false
            val receiverClass = dispatchReceiver?.type?.classOrNull?.owner
            return receiverClass?.canUseSpecializedEqMethod == true
        }

    private val IrCall.isEqEqCallOnInlineClass: Boolean
        get() {
            // Note that reference equality (x === y) is not allowed on values of inline class type,
            // so it is enough to check for eqeq.
            if (symbol != context.irBuiltIns.eqeqSymbol) return false
            val leftOperandClass = arguments[0]?.type?.classOrNull?.owner ?: return false
            return leftOperandClass.canUseSpecializedEqMethod
        }

    private val IrClass.canUseSpecializedEqMethod: Boolean
        get() {
            if (!isSingleFieldValueClass) return false
            // Before version 1.4, we cannot rely on the Result.equals-impl0 method
            return !isClassWithFqName(StandardNames.RESULT_FQ_NAME) ||
                    context.config.languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4
        }

    override fun visitGetField(expression: IrGetField): IrExpression {
        val field = expression.symbol.owner
        val parent = field.parent
        if (field.origin == IrDeclarationOrigin.PROPERTY_BACKING_FIELD &&
            parent is IrClass &&
            parent.isSingleFieldValueClass &&
            field.name == parent.inlineClassFieldName
        ) {
            val receiver = expression.receiver!!.transform(this, null)
            // If we get the field of nullable variable, we can be sure, that type is not null,
            // since we first generate null check.
            return coerceInlineClasses(receiver, receiver.type.makeNotNull(), field.type)
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
        val primaryConstructor = valueClass.addConstructor {
            updateFrom(irConstructor)
            visibility = DescriptorVisibilities.PRIVATE
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER
        }.apply {
            copyFunctionSignatureFrom(irConstructor)
            // Don't create a default argument stub for the primary constructor
            parameters.forEach { it.defaultValue = null }
            if (irConstructor.shouldBeExposedByAnnotationOrFlag(context.config.languageVersionSettings)) {
                addValueParameter {
                    origin = INLINE_CLASS_CONSTRUCTOR_SYNTHETIC_PARAMETER
                    name = Name.identifier("\$null")
                    type = context.symbols.boxingConstructorMarkerClass.defaultType
                }
            }
            // Only exposed declarations should be annotated with @JvmExposeBoxed in bytecode
            annotations = irConstructor.annotations.withoutJvmExposeBoxedAnnotation()
            body = context.createIrBuilder(this.symbol).irBlockBody(this) {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                +irSetField(
                    irGet(valueClass.thisReceiver!!),
                    getInlineClassBackingField(valueClass),
                    irGet(this@apply.parameters[0])
                )
            }
        }

        // Add a static bridge method to the primary constructor. This contains
        // null-checks, default arguments, and anonymous initializers.
        val function = context.inlineClassReplacements.getReplacementFunction(irConstructor)!!

        val initBlocks = valueClass.declarations.filterIsInstance<IrAnonymousInitializer>()
            .filterNot { it.isStatic }

        function.parameters.forEach { it.transformChildrenVoid() }
        function.body = context.createIrBuilder(function.symbol).irBlockBody {
            val argument = function.parameters[0]
            val thisValue = irTemporary(coerceInlineClasses(irGet(argument), argument.type, function.returnType, skipCast = true))
            valueMap[valueClass.thisReceiver!!.symbol] = thisValue
            for (initBlock in initBlocks) {
                for (stmt in initBlock.body.statements) {
                    +stmt.transformStatement(this@JvmInlineClassLowering).patchDeclarationParents(function)
                }
            }
            +irReturn(irGet(thisValue))
        }

        valueClass.declarations.removeAll(initBlocks)
        valueClass.declarations += function

        if (irConstructor.shouldBeExposedByAnnotationOrFlag(context.config.languageVersionSettings)) {
            valueClass.addExposedForJavaConstructor(irConstructor, primaryConstructor, function)
        }
    }

    private fun IrClass.addExposedForJavaConstructor(
        irConstructor: IrConstructor,
        primaryConstructor: IrConstructor,
        function: IrSimpleFunction,
    ) {
        addConstructor {
            updateFrom(irConstructor)
            isPrimary = false
            origin = JvmLoweredDeclarationOrigin.EXPOSED_INLINE_CLASS_CONSTRUCTOR
        }.apply {
            // Don't create a default argument stub for the exposed constructor
            copyFunctionSignatureFrom(irConstructor)
            parameters.forEach { it.defaultValue = null }
            annotations = irConstructor.annotations.withJvmExposeBoxedAnnotation(irConstructor, this@JvmInlineClassLowering.context)
            body = this@JvmInlineClassLowering.context.createIrBuilder(symbol).irBlockBody(this) {
                // Call private constructor
                +irDelegatingConstructorCall(primaryConstructor).apply {
                    passTypeArgumentsFrom(primaryConstructor)
                    arguments[0] = irGet(parameters[0])
                    arguments[1] = irNull()
                }
                // Call constructor-impl to run init blocks
                +irCall(function).apply {
                    arguments[0] = irGet(parameters[0])
                    passTypeArgumentsFrom(primaryConstructor)
                }
            }
        }

        // Add no-arg constructor for usage by something like Hibernate
        val defaultValue = irConstructor.parameters.singleOrNull()?.defaultValue
        if (defaultValue != null) {
            addConstructor {
                updateFrom(irConstructor)
                isPrimary = false
                origin = JvmLoweredDeclarationOrigin.EXPOSED_INLINE_CLASS_CONSTRUCTOR
                returnType = irConstructor.returnType
            }.apply {
                copyTypeParametersFrom(irConstructor)
                annotations = irConstructor.annotations.withJvmExposeBoxedAnnotation(irConstructor, this@JvmInlineClassLowering.context)
                body = this@JvmInlineClassLowering.context.createIrBuilder(symbol).irBlockBody(this) {
                    // Delegate to exposed constructor-impl$default
                    val tmp = irTemporary(irCall(function).apply {
                        arguments[0] = null
                        passTypeArgumentsFrom(primaryConstructor)
                    })
                    +irDelegatingConstructorCall(primaryConstructor).apply {
                        passTypeArgumentsFrom(primaryConstructor)
                        arguments[0] = irGet(tmp).coerceToUnboxed()
                        arguments[1] = irNull()
                    }
                }
            }
        }
    }

    private fun List<IrConstructorCall>.withoutJvmNameAnnotation(): List<IrConstructorCall> =
        this.toMutableList().apply {
            removeAll {
                it.symbol.owner.returnType.classOrNull?.owner?.hasEqualFqName(JVM_NAME_ANNOTATION_FQ_NAME) == true
            }
        }

    private fun buildBoxFunction(valueClass: IrClass) {
        val function = context.inlineClassReplacements.getBoxFunction(valueClass)
        with(context.createIrBuilder(function.symbol)) {
            function.body = irExprBody(
                irCall(valueClass.primaryConstructor!!.symbol).apply {
                    passTypeArgumentsFrom(function)
                    arguments[0] = irGet(function.parameters[0])
                    if (valueClass.primaryConstructor?.parameters?.last()?.origin == INLINE_CLASS_CONSTRUCTOR_SYNTHETIC_PARAMETER) {
                        arguments[1] = irNull()
                    }
                }
            )
        }
        valueClass.declarations += function
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

    private fun buildSpecializedEqualsMethodIfNeeded(valueClass: IrClass) {
        val function = context.inlineClassReplacements.getSpecializedEqualsMethod(valueClass, context.irBuiltIns)
        // Return if we have already built specialized equals as static replacement of typed equals
        if (function.body != null) return
        val (left, right) = function.parameters
        val type = left.type.unboxInlineClass()

        val untypedEquals = valueClass.functions.single { it.isEquals() }

        function.body = context.createIrBuilder(valueClass.symbol).run {
            val context = this@JvmInlineClassLowering.context
            val underlyingType = getInlineClassUnderlyingType(valueClass)
            irExprBody(
                if (untypedEquals.origin == IrDeclarationOrigin.DEFINED) {
                    val boxFunction = context.inlineClassReplacements.getBoxFunction(valueClass)

                    fun irBox(expr: IrExpression) = irCall(boxFunction).apply { arguments[0] = expr }

                    irCall(untypedEquals).apply {
                        arguments[0] = irBox(coerceInlineClasses(irGet(left), left.type, underlyingType))
                        arguments[1] = irBox(coerceInlineClasses(irGet(right), right.type, underlyingType))
                    }
                } else {
                    val underlyingClass = underlyingType.getClass()
                    // We can't directly compare unboxed values of underlying inline class as this class can have custom equals
                    if (underlyingClass?.isSingleFieldValueClass == true && !underlyingType.isNullable()) {
                        val underlyingClassEq =
                            context.inlineClassReplacements.getSpecializedEqualsMethod(underlyingClass, context.irBuiltIns)
                        irCall(underlyingClassEq).apply {
                            arguments[0] = coerceInlineClasses(irGet(left), left.type, underlyingType)
                            arguments[1] = coerceInlineClasses(irGet(right), right.type, underlyingType)
                        }
                    } else {
                        irEquals(coerceInlineClasses(irGet(left), left.type, type), coerceInlineClasses(irGet(right), right.type, type))
                    }
                }
            )
        }

        valueClass.declarations += function
    }
}
