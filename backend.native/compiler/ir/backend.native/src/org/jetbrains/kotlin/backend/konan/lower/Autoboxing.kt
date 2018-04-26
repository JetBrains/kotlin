/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.AbstractValueUsageTransformer
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.target
import org.jetbrains.kotlin.backend.konan.irasdescriptors.containsNull
import org.jetbrains.kotlin.backend.konan.irasdescriptors.isOverridable
import org.jetbrains.kotlin.backend.konan.irasdescriptors.isSuspend
import org.jetbrains.kotlin.backend.konan.irasdescriptors.makeNullableAsSpecified
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Boxes and unboxes values of value types when necessary.
 */
internal class Autoboxing(val context: Context) : FileLoweringPass {

    private val transformer = AutoboxingTransformer(context)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(transformer)
    }

}

private class AutoboxingTransformer(val context: Context) : AbstractValueUsageTransformer(
        context.builtIns,
        context.ir.symbols,
        context.irBuiltIns
) {

    // TODO: should we handle the cases when expression type
    // is not equal to e.g. called function return type?


    /**
     * @return type to use for runtime type checks instead of given one (e.g. `IntBox` instead of `Int`)
     */
    private fun getRuntimeReferenceType(type: IrType): IrType {
        ValueType.values().forEach {
            if (type.notNullableIsRepresentedAs(it)) {
                return getBoxType(it).makeNullableAsSpecified(type.containsNull())
            }
        }

        return type
    }

    override fun IrExpression.useInTypeOperator(operator: IrTypeOperator, typeOperand: IrType): IrExpression {
        return if (operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT ||
                   operator == IrTypeOperator.IMPLICIT_INTEGER_COERCION) {
            this
        } else {
            // Codegen expects the argument of type-checking operator to be an object reference:
            this.useAs(context.irBuiltIns.anyNType)
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        super.visitTypeOperator(expression).let {
            // Assume that the transformer doesn't replace the entire expression for simplicity:
            assert (it === expression)
        }

        val newTypeOperand = getRuntimeReferenceType(expression.typeOperand)
        val newTypeOperandClassifier = newTypeOperand.classifierOrFail

        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> expression

            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST,
            IrTypeOperator.IMPLICIT_NOTNULL, IrTypeOperator.SAFE_CAST -> {

                val newExpressionType = if (expression.operator == IrTypeOperator.SAFE_CAST) {
                    newTypeOperand.makeNullable()
                } else {
                    newTypeOperand
                }

                IrTypeOperatorCallImpl(expression.startOffset, expression.endOffset,
                        newExpressionType, expression.operator, newTypeOperand, newTypeOperandClassifier,
                        expression.argument).useAs(expression.type)
            }

            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> if (newTypeOperand == expression.typeOperand) {
                // Do not create new expression if nothing changes:
                expression
            } else {
                IrTypeOperatorCallImpl(expression.startOffset, expression.endOffset,
                        expression.type, expression.operator, newTypeOperand, newTypeOperandClassifier,
                        expression.argument)
            }
        }
    }

    private var currentFunctionDescriptor: IrFunction? = null

    override fun visitFunction(declaration: IrFunction): IrStatement {
        currentFunctionDescriptor = declaration
        val result = super.visitFunction(declaration)
        currentFunctionDescriptor = null
        return result
    }

    override fun IrExpression.useAsReturnValue(returnTarget: IrReturnTargetSymbol): IrExpression = when (returnTarget) {
        is IrSimpleFunctionSymbol -> if (returnTarget.owner.isSuspend && returnTarget == currentFunctionDescriptor?.symbol) {
            this.useAs(irBuiltIns.anyNType)
        } else {
            this.useAs(returnTarget.owner.returnType)
        }
        is IrConstructorSymbol -> this.useAs(irBuiltIns.unitType)
        is IrReturnableBlockSymbol -> this.useAs(returnTarget.owner.type)
        else -> error(returnTarget)
    }

    override fun IrExpression.useAs(type: IrType): IrExpression {
        val interop = context.interopBuiltIns
        if (this.isNullConst() && interop.nullableInteropValueTypes.any { type.isRepresentedAs(it) }) {
            return IrCallImpl(
                    startOffset,
                    endOffset,
                    symbols.getNativeNullPtr.owner.returnType,
                    symbols.getNativeNullPtr
            ).uncheckedCast(type)
        }

        val actualType = when (this) {
            is IrCall -> {
                if (this.symbol.owner.isSuspend) irBuiltIns.anyNType
                else this.callTarget.returnType
            }
            is IrGetField -> this.symbol.owner.type

            is IrTypeOperatorCall -> when (this.operator) {
                IrTypeOperator.IMPLICIT_INTEGER_COERCION ->
                    // TODO: is it a workaround for inconsistent IR?
                    this.typeOperand

                else -> this.type
            }

            else -> this.type
        }

        return this.adaptIfNecessary(actualType, type)
    }

    private val IrFunctionAccessExpression.target: IrFunction get() = when (this) {
        is IrCall -> this.callTarget
        is IrDelegatingConstructorCall -> this.symbol.owner
        else -> TODO(this.render())
    }

    private val IrCall.callTarget: IrFunction
        get() = if (superQualifier == null && symbol.owner.isOverridable) {
            // A virtual call.
            symbol.owner
        } else {
            symbol.owner.target
        }

    override fun IrExpression.useAsDispatchReceiver(expression: IrFunctionAccessExpression): IrExpression {
        return this.useAsArgument(expression.target.dispatchReceiverParameter!!)
    }

    override fun IrExpression.useAsExtensionReceiver(expression: IrFunctionAccessExpression): IrExpression {
        return this.useAsArgument(expression.target.extensionReceiverParameter!!)
    }

    override fun IrExpression.useAsValueArgument(expression: IrFunctionAccessExpression,
                                                 parameter: IrValueParameter): IrExpression {

        return this.useAsArgument(expression.target.valueParameters[parameter.index])
    }

    private fun IrExpression.adaptIfNecessary(actualType: IrType, expectedType: IrType): IrExpression {
        val conversion = symbols.getTypeConversion(actualType, expectedType)
        return if (conversion == null) {
            this
        } else {
            val parameter = conversion.owner.explicitParameters.single()
            val argument = this.uncheckedCast(parameter.type)

            IrCallImpl(startOffset, endOffset, conversion.owner.returnType, conversion).apply {
                addArguments(mapOf(parameter to argument))
            }.uncheckedCast(this.type) // Try not to bring new type incompatibilities.
        }
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        expression.transformChildrenVoid()
        assert(expression.getArgumentsWithIr().isEmpty())
        return expression
    }

    /**
     * Casts this expression to `type` without changing its representation in generated code.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun IrExpression.uncheckedCast(type: IrType): IrExpression {
        // TODO: apply some cast if types are incompatible; not required currently.
        return this
    }

    private fun getBoxType(valueType: ValueType) = symbols.boxClasses[valueType]!!.owner.defaultType

}
