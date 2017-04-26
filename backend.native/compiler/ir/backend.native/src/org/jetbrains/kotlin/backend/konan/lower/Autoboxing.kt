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
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ValueType
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.notNullableIsRepresentedAs
import org.jetbrains.kotlin.backend.konan.isRepresentedAs
import org.jetbrains.kotlin.backend.konan.util.atMostOne
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.makeNullable

/**
 * Boxes and unboxes values of value types when necessary.
 */
internal class Autoboxing(val context: Context) : FileLoweringPass {

    private val transformer = AutoboxingTransformer(context)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(transformer)
    }

}

private class AutoboxingTransformer(val context: Context) : AbstractValueUsageTransformer(context.builtIns) {

    // TODO: should we handle the cases when expression type
    // is not equal to e.g. called function return type?


    /**
     * @return type to use for runtime type checks instead of given one (e.g. `IntBox` instead of `Int`)
     */
    private fun getRuntimeReferenceType(type: KotlinType): KotlinType {
        ValueType.values().forEach {
            if (type.notNullableIsRepresentedAs(it)) {
                return getBoxType(it).makeNullableAsSpecified(TypeUtils.isNullableType(type))
            }
        }

        return type
    }

    override fun IrExpression.useInTypeOperator(operator: IrTypeOperator, typeOperand: KotlinType): IrExpression {
        return if (operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT ||
                   operator == IrTypeOperator.IMPLICIT_INTEGER_COERCION) {
            this
        } else {
            // Codegen expects the argument of type-checking operator to be an object reference:
            this.useAs(builtIns.nullableAnyType)
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        super.visitTypeOperator(expression).let {
            // Assume that the transformer doesn't replace the entire expression for simplicity:
            assert (it === expression)
        }

        val newTypeOperand = getRuntimeReferenceType(expression.typeOperand)

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
                        newExpressionType, expression.operator, newTypeOperand,
                        expression.argument).useAs(expression.type)
            }

            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> if (newTypeOperand == expression.typeOperand) {
                // Do not create new expression if nothing changes:
                expression
            } else {
                IrTypeOperatorCallImpl(expression.startOffset, expression.endOffset,
                        expression.type, expression.operator, newTypeOperand, expression.argument)
            }
        }
    }

    /**
     * @return the [ValueType] given type represented in generated code as,
     * or `null` if represented as object reference.
     */
    private fun getValueType(type: KotlinType): ValueType? {
        return ValueType.values().firstOrNull {
            type.isRepresentedAs(it)
        }
    }

    private var currentFunctionDescriptor: FunctionDescriptor? = null

    override fun visitFunction(declaration: IrFunction): IrStatement {
        currentFunctionDescriptor = declaration.descriptor
        val result = super.visitFunction(declaration)
        currentFunctionDescriptor = null
        return result
    }

    override fun IrExpression.useAsReturnValue(returnTarget: CallableDescriptor): IrExpression {
        if (returnTarget.isSuspend && returnTarget == currentFunctionDescriptor)
            return this.useAs(context.builtIns.nullableAnyType)
        val returnType = returnTarget.returnType
                ?: return this
        return this.useAs(returnType)
    }

    override fun IrExpression.useAs(type: KotlinType): IrExpression {
        val interop = context.interopBuiltIns
        if (this.isNullConst() && interop.nullableInteropValueTypes.any { type.isRepresentedAs(it) }) {
            return IrCallImpl(startOffset, endOffset, context.builtIns.getNativeNullPtr).uncheckedCast(type)
        }

        val actualType = when (this) {
            is IrCall -> {
                if (this.descriptor.isSuspend) context.builtIns.nullableAnyType
                else this.descriptor.original.returnType ?: this.type
            }
            is IrGetField -> this.descriptor.original.type

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

    override fun IrExpression.useAsDispatchReceiver(function: CallableDescriptor): IrExpression {
        return this.useAsArgument(function.original.dispatchReceiverParameter!!)
    }

    override fun IrExpression.useAsExtensionReceiver(function: CallableDescriptor): IrExpression {
        return this.useAsArgument(function.original.extensionReceiverParameter!!)
    }

    override fun IrExpression.useAsValueArgument(parameter: ValueParameterDescriptor): IrExpression {
        val function = parameter.containingDeclaration
        return this.useAsArgument(function.original.valueParameters[parameter.index])
    }

    override fun IrExpression.useForField(field: PropertyDescriptor): IrExpression {
        return this.useForVariable(field.original)
    }

    private fun IrExpression.adaptIfNecessary(actualType: KotlinType, expectedType: KotlinType): IrExpression {
        val actualValueType = getValueType(actualType)
        val expectedValueType = getValueType(expectedType)

        return when {
            actualValueType == expectedValueType -> this

            actualValueType == null && expectedValueType != null -> {
                // This may happen in the following cases:
                // 1.  `actualType` is `Nothing`;
                // 2.  `actualType` is incompatible.

                this.unbox(expectedValueType)
            }

            actualValueType != null && expectedValueType == null -> this.box(actualValueType)

            else -> throw IllegalArgumentException("actual type is $actualType, expected $expectedType")
        }
    }

    /**
     * Casts this expression to `type` without changing its representation in generated code.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun IrExpression.uncheckedCast(type: KotlinType): IrExpression {
        // TODO: apply some cast if types are incompatible; not required currently.
        return this
    }

    private val ValueType.shortName
        get() = this.classFqName.shortName()

    private fun getBoxType(valueType: ValueType) =
            context.builtIns.getKonanInternalClass("${valueType.shortName}Box").defaultType

    private fun IrExpression.box(valueType: ValueType): IrExpression {
        val boxFunctionName = "box${valueType.shortName}"
        val boxFunction = context.builtIns.getKonanInternalFunctions(boxFunctionName).singleOrNull() ?:
                TODO(valueType.toString())

        return IrCallImpl(startOffset, endOffset, boxFunction).apply {
            putValueArgument(0, this@box)
        }.uncheckedCast(this.type) // Try not to bring new type incompatibilities.
    }

    private fun IrExpression.unbox(valueType: ValueType): IrExpression {
        val unboxFunctionName = "unbox${valueType.shortName}"

        context.builtIns.getKonanInternalFunctions(unboxFunctionName).atMostOne()?.let {
            return IrCallImpl(startOffset, endOffset, it).apply {
                putValueArgument(0, this@unbox.uncheckedCast(it.valueParameters[0].type))
            }.uncheckedCast(this.type)
        }

        val boxGetter = getBoxType(valueType)
                .memberScope.getContributedDescriptors()
                .filterIsInstance<PropertyDescriptor>()
                .single { it.name.asString() == "value" }
                .getter!!

        return IrCallImpl(startOffset, endOffset, boxGetter).apply {
            dispatchReceiver = this@unbox.uncheckedCast(boxGetter.dispatchReceiverParameter!!.type)
        }.uncheckedCast(this.type) // Try not to bring new type incompatibilities.
    }

}
