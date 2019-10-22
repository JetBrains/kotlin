/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.*
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetVariableImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

val jvmInlineClassPhase = makeIrFilePhase(
    ::JvmInlineClassLowering,
    name = "Inline Classes",
    description = "Lower inline classes"
)

/**
 * Adds new constructors, box, and unbox functions to inline classes as well as replacement
 * functions and bridges to avoid clashes between overloaded function. Changes calls with
 * known types to call the replacement functions.
 *
 * We do not unfold inline class types here. Instead, the type mapper will lower inline class
 * types to the types of their underlying field.
 */
private class JvmInlineClassLowering(private val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    private val valueMap = mutableMapOf<IrValueSymbol, IrValueDeclaration>()

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        // The arguments to the primary constructor are in scope in the initializers of IrFields.
        declaration.primaryConstructor?.let {
            context.inlineClassReplacements.getReplacementFunction(it)?.let { replacement ->
                valueMap.putAll(replacement.valueParameterMap)
            }
        }

        declaration.transformDeclarationsFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction) {
                transformFunctionFlat(memberDeclaration)
            } else {
                memberDeclaration.accept(this, null)
                null
            }
        }

        if (declaration.isInline) {
            val irConstructor = declaration.primaryConstructor!!
            // The field getter is used by reflection and cannot be removed here.
            declaration.declarations.remove(irConstructor)
            buildPrimaryInlineClassConstructor(declaration, irConstructor)
            buildBoxFunction(declaration)
            buildUnboxFunction(declaration)
            buildSpecializedEqualsMethod(declaration)
        }

        return declaration
    }

    private fun transformFunctionFlat(function: IrFunction): List<IrDeclaration>? {
        if (function.isPrimaryInlineClassConstructor)
            return null

        val replacement = context.inlineClassReplacements.getReplacementFunction(function)
        if (replacement == null) {
            function.transformChildrenVoid()
            return null
        }

        return when (function) {
            is IrSimpleFunction -> transformSimpleFunctionFlat(function, replacement)
            is IrConstructor -> transformConstructorFlat(function, replacement)
            else -> throw IllegalStateException()
        }
    }

    private fun transformSimpleFunctionFlat(function: IrSimpleFunction, replacement: IrReplacementFunction): List<IrDeclaration> {
        val worker = replacement.function
        valueMap.putAll(replacement.valueParameterMap)
        worker.valueParameters.forEach { it.transformChildrenVoid() }
        worker.body = function.body?.transform(this, null)?.patchDeclarationParents(worker)

        // Don't create a wrapper for functions which are only used in an unboxed context
        if (function.overriddenSymbols.isEmpty() || worker.dispatchReceiverParameter != null)
            return listOf(worker)

        // Replace the function body with a wrapper
        context.createIrBuilder(function.symbol, function.startOffset, function.endOffset).run {
            val call = irCall(worker).apply {
                passTypeArgumentsFrom(function)
                for (parameter in function.explicitParameters) {
                    val newParameter = replacement.valueParameterMap[parameter.symbol] ?: continue
                    putArgument(newParameter, irGet(parameter))
                }
            }

            function.body = irExprBody(call)
        }

        return listOf(worker, function)
    }

    // Secondary constructors for boxed types get translated to static functions returning
    // unboxed arguments. We remove the original constructor.
    private fun transformConstructorFlat(constructor: IrConstructor, replacement: IrReplacementFunction): List<IrDeclaration> {
        val worker = replacement.function

        valueMap.putAll(replacement.valueParameterMap)
        worker.valueParameters.forEach { it.transformChildrenVoid() }
        worker.body = context.createIrBuilder(worker.symbol, worker.startOffset, worker.endOffset).irBlockBody(worker) {
            val thisVar = irTemporaryVarDeclaration(
                worker.returnType, nameHint = "\$this", isMutable = false
            )
            valueMap[constructor.constructedClass.thisReceiver!!.symbol] = thisVar

            constructor.body?.statements?.forEach { statement ->
                +statement
                    .transform(object : IrElementTransformerVoid() {
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
                            return irSetVar(thisVar.symbol, expression)
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
                    }, null)
                    .transform(this@JvmInlineClassLowering, null)
                    .patchDeclarationParents(worker)
            }

            +irReturn(irGet(thisVar))
        }

        return listOf(worker)
    }

    private fun typedArgumentList(function: IrFunction, expression: IrMemberAccessExpression) =
        listOfNotNull(
            function.dispatchReceiverParameter?.let { it to expression.dispatchReceiver },
            function.extensionReceiverParameter?.let { it to expression.extensionReceiver }
        ) + function.valueParameters.map { it to expression.getValueArgument(it.index) }

    private fun IrMemberAccessExpression.buildReplacement(
        originalFunction: IrFunction,
        original: IrMemberAccessExpression,
        replacement: IrReplacementFunction
    ) {
        copyTypeArgumentsFrom(original)
        for ((parameter, argument) in typedArgumentList(originalFunction, original)) {
            if (argument == null) continue
            val newParameter = replacement.valueParameterMap[parameter.symbol] ?: continue
            putArgument(
                replacement.function,
                newParameter,
                argument.transform(this@JvmInlineClassLowering, null)
            )
        }
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val replacement = context.inlineClassReplacements.getReplacementFunction(expression.symbol.owner)
            ?: return super.visitFunctionReference(expression)
        val function = replacement.function

        return IrFunctionReferenceImpl(
            expression.startOffset, expression.endOffset, expression.type,
            function.symbol, function.typeParameters.size,
            function.valueParameters.size, expression.origin
        ).apply {
            buildReplacement(expression.symbol.owner, expression, replacement)
        }.copyAttributes(expression)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        val function = expression.symbol.owner
        val replacement = context.inlineClassReplacements.getReplacementFunction(function)
            ?: return super.visitFunctionAccess(expression)
        return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
            .irCall(replacement.function).apply {
                buildReplacement(function, expression, replacement)
            }
    }

    private fun coerceInlineClasses(argument: IrExpression, from: IrType, to: IrType) =
        IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, to, context.ir.symbols.unsafeCoerceIntrinsic).apply {
            putTypeArgument(0, from)
            putTypeArgument(1, to)
            putValueArgument(0, argument)
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
                this@JvmInlineClassLowering.context.inlineClassReplacements.getReplacementFunction(equals)!!.function
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
                coerceInlineClasses(arg, expression.symbol.owner.dispatchReceiverParameter!!.type, expression.type)
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
            return (leftClass.fqNameWhenAvailable != DescriptorUtils.RESULT_FQ_NAME) ||
                    context.state.languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4
        }

    override fun visitGetField(expression: IrGetField): IrExpression {
        val field = expression.symbol.owner
        val parent = field.parent
        if (parent is IrClass && parent.isInline && field.name == parent.inlineClassFieldName) {
            val receiver = expression.receiver!!.transform(this, null)
            return coerceInlineClasses(receiver, receiver.type, field.type)
        }
        return super.visitGetField(expression)
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        expression.returnTargetSymbol.owner.safeAs<IrFunction>()?.let { target ->
            context.inlineClassReplacements.getReplacementFunction(target)?.let {
                return context.createIrBuilder(it.function.symbol, expression.startOffset, expression.endOffset).irReturn(
                    expression.value.transform(this, null)
                )
            }
        }
        return super.visitReturn(expression)
    }

    private fun visitStatementContainer(container: IrStatementContainer) {
        container.statements.transformFlat { statement ->
            if (statement is IrFunction)
                transformFunctionFlat(statement)
            else
                listOf(statement.transform(this, null))
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        visitStatementContainer(expression)
        return expression
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {
        visitStatementContainer(body)
        return body
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

    override fun visitSetVariable(expression: IrSetVariable): IrExpression {
        valueMap[expression.symbol]?.let {
            return IrSetVariableImpl(
                expression.startOffset, expression.endOffset,
                it.type, it.symbol as IrVariableSymbol, expression.origin
            ).apply {
                value = expression.value.transform(this@JvmInlineClassLowering, null)
            }
        }
        return super.visitSetVariable(expression)
    }

    private fun buildPrimaryInlineClassConstructor(irClass: IrClass, irConstructor: IrConstructor) {
        // Add the default primary constructor
        irClass.addConstructor {
            updateFrom(irConstructor)
            visibility = Visibilities.PRIVATE
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER
            returnType = irConstructor.returnType
        }.apply {
            copyParameterDeclarationsFrom(irConstructor)
            body = context.createIrBuilder(this.symbol).irBlockBody(this) {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                +irSetField(
                    irGet(irClass.thisReceiver!!),
                    getInlineClassBackingField(irClass),
                    irGet(this@apply.valueParameters[0])
                )
            }
        }

        // Add a static bridge method to the primary constructor.
        // This is a placeholder for null-checks and default arguments.
        val function = context.inlineClassReplacements.getReplacementFunction(irConstructor)!!.function
        with(context.createIrBuilder(function.symbol)) {
            val argument = function.valueParameters[0]
            function.body = irExprBody(
                coerceInlineClasses(irGet(argument), argument.type, function.returnType)
            )
        }
        irClass.declarations += function
    }

    private fun buildBoxFunction(irClass: IrClass) {
        val function = context.inlineClassReplacements.getBoxFunction(irClass)
        with(context.createIrBuilder(function.symbol)) {
            function.body = irExprBody(
                irCall(irClass.primaryConstructor!!.symbol).apply {
                    passTypeArgumentsFrom(function)
                    putValueArgument(0, irGet(function.valueParameters[0]))
                }
            )
        }
        irClass.declarations += function
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

    private fun buildSpecializedEqualsMethod(irClass: IrClass) {
        val function = context.inlineClassReplacements.getSpecializedEqualsMethod(irClass, context.irBuiltIns)
        val left = function.valueParameters[0]
        val right = function.valueParameters[1]
        val type = left.type.unboxInlineClass()

        function.body = context.createIrBuilder(irClass.symbol).run {
            irExprBody(
                irEquals(
                    coerceInlineClasses(irGet(left), left.type, type),
                    coerceInlineClasses(irGet(right), right.type, type)
                )
            )
        }

        irClass.declarations += function
    }
}
