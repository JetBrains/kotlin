package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.AbstractValueUsageTransformer
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.getKonanInternalClass
import org.jetbrains.kotlin.backend.konan.getKonanInternalFunctions
import org.jetbrains.kotlin.backend.konan.unboundCallableReferenceTypeOrNull
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.singletonOrEmptyList

/**
 * Boxes and unboxes values of primitive types when necessary.
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


    private val irBuiltins = context.irModule!!.irBuiltins

    /**
     * The list of primitive types to box and unbox.
     */
    private val primitiveTypes = with(context.builtIns) {
        listOf(booleanType, byteType, shortType, charType, intType, longType, floatType, doubleType) +
                unboundCallableReferenceTypeOrNull.singletonOrEmptyList()
    }

    /**
     * @return type to use for runtime type checks instead of given one (e.g. `IntBox` instead of `Int`)
     */
    private fun getRuntimeReferenceType(type: KotlinType): KotlinType {
        if (type.isSubtypeOf(builtIns.nullableNothingType)) return type

        primitiveTypes.forEach {
            listOf(it, it.makeNullable()).forEach { superType ->
                if (type.isSubtypeOf(superType)) {
                    return getBoxType(superType).makeNullableAsSpecified(superType.isMarkedNullable)
                }
            }
        }

        return type
    }

    override fun IrExpression.useInTypeOperator(operator: IrTypeOperator, typeOperand: KotlinType): IrExpression {
        return if (operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
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
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> expression

            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST,
            IrTypeOperator.IMPLICIT_NOTNULL, IrTypeOperator.SAFE_CAST -> {

                // Codegen produces the object reference:
                val newExpressionType = builtIns.nullableAnyType

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

    override fun IrExpression.useAsArgument(parameter: ParameterDescriptor): IrExpression {
        return if (parameter.containingDeclaration == irBuiltins.eqeq) {
            this // Do not box arguments of `==` because codegen expects them to be unboxed.
            // TODO: implement `==` lowering at IR level and remove this hack.
        } else {
            this.useAsValue(parameter)
        }
    }

    /**
     * @return the element of [primitiveTypes] if given type is represented as primitive type in generated code,
     * or `null` if represented as object reference.
     */
    private fun getCustomRepresentation(type: KotlinType): KotlinType? {
        if (type.isSubtypeOf(builtIns.nothingType)) return null

        return primitiveTypes.firstOrNull { type.isSubtypeOf(it) }
    }

    override fun IrExpression.useAs(type: KotlinType): IrExpression {
        return this.adaptIfNecessary(type)
    }

    private fun IrExpression.adaptIfNecessary(expectedType: KotlinType): IrExpression {
        val thisRepresentation = getCustomRepresentation(this.type)
        val expectedRepresentation = getCustomRepresentation(expectedType)

        return when {
            thisRepresentation == expectedRepresentation -> this

            thisRepresentation == null && expectedRepresentation != null -> {
                // This may happen in the following cases:
                // 1.  `this.type` is `Nothing`;
                // 2.  `this` has the incompatible type.

                this.unbox(expectedRepresentation)
            }

            thisRepresentation != null && expectedRepresentation == null -> this.box(thisRepresentation)

            else -> throw IllegalArgumentException("this is ${this.type}, expected $expectedType")
        }
    }

    /**
     * Casts this expression to `type` without changing its representation in generated code.
     */
    private fun IrExpression.uncheckedCast(type: KotlinType): IrExpression {
        // TODO: apply some cast if types are incompatible; not required currently.
        return this
    }

    private fun getBoxType(primitiveType: KotlinType): SimpleType {
        val primitiveTypeClass = TypeUtils.getClassDescriptor(primitiveType)!!
        return context.builtIns.getKonanInternalClass("${primitiveTypeClass.name}Box").defaultType
    }

    private fun IrExpression.box(primitiveType: KotlinType): IrExpression {
        val primitiveTypeClass = TypeUtils.getClassDescriptor(primitiveType)!!
        val boxFunction = context.builtIns.getKonanInternalFunctions("box${primitiveTypeClass.name}").single()

        return IrCallImpl(startOffset, endOffset, boxFunction).apply {
            putValueArgument(0, this@box)
        }.uncheckedCast(this.type) // Try not to bring new type incompatibilities.
    }

    private fun IrExpression.unbox(primitiveType: KotlinType): IrExpression {
        val boxGetter = getBoxType(primitiveType)
                .memberScope.getContributedDescriptors()
                .filterIsInstance<PropertyDescriptor>()
                .single { it.name.asString() == "value" }
                .getter!!

        return IrCallImpl(startOffset, endOffset, boxGetter).apply {
            dispatchReceiver = this@unbox.uncheckedCast(boxGetter.dispatchReceiverParameter!!.type)
        }.uncheckedCast(this.type) // Try not to bring new type incompatibilities.
    }

}