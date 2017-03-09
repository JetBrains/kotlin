package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable

typealias ReportError = (element: IrElement, message: String) -> Unit

class CheckIrElementVisitor(val builtIns: KotlinBuiltIns, val reportError: ReportError) : IrElementVisitorVoid {

    override fun visitElement(element: IrElement) {
        // Nothing to do.
    }

    private fun IrExpression.ensureTypeIs(expectedType: KotlinType) {
        if (expectedType != type) {
            reportError(this, "unexpected expression.type: expected $expectedType, got ${type}")
        }
    }

    override fun <T> visitConst(expression: IrConst<T>) {
        super.visitConst(expression)

        val naturalType = when (expression.kind) {
            IrConstKind.Null -> builtIns.nullableNothingType
            IrConstKind.Boolean -> builtIns.booleanType
            IrConstKind.Char -> builtIns.charType
            IrConstKind.Byte -> builtIns.byteType
            IrConstKind.Short -> builtIns.shortType
            IrConstKind.Int -> builtIns.intType
            IrConstKind.Long -> builtIns.longType
            IrConstKind.String -> builtIns.stringType
            IrConstKind.Float -> builtIns.floatType
            IrConstKind.Double -> builtIns.doubleType
        }

        expression.ensureTypeIs(naturalType)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation) {
        super.visitStringConcatenation(expression)

        expression.ensureTypeIs(builtIns.stringType)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue) {
        super.visitGetObjectValue(expression)

        expression.ensureTypeIs(expression.descriptor.defaultType)
    }

    // TODO: visitGetEnumValue

    override fun visitGetValue(expression: IrGetValue) {
        super.visitGetValue(expression)

        expression.ensureTypeIs(expression.descriptor.type)
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        super.visitSetVariable(expression)

        expression.ensureTypeIs(builtIns.unitType)
    }

    override fun visitGetField(expression: IrGetField) {
        super.visitGetField(expression)

        expression.ensureTypeIs(expression.descriptor.type)
    }

    override fun visitSetField(expression: IrSetField) {
        super.visitSetField(expression)

        expression.ensureTypeIs(builtIns.unitType)
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)

        val returnType = expression.descriptor.returnType
        if (returnType == null) {
            reportError(expression, "${expression.descriptor} return type is null")
        } else {
            expression.ensureTypeIs(returnType)
        }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        super.visitDelegatingConstructorCall(expression)

        expression.ensureTypeIs(builtIns.unitType)
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) {
        super.visitEnumConstructorCall(expression)

        expression.ensureTypeIs(builtIns.unitType)
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        super.visitInstanceInitializerCall(expression)

        expression.ensureTypeIs(builtIns.unitType)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        super.visitTypeOperator(expression)

        val operator = expression.operator
        val typeOperand = expression.typeOperand

        val naturalType = when (operator) {
            IrTypeOperator.CAST,
            IrTypeOperator.IMPLICIT_CAST,
            IrTypeOperator.IMPLICIT_NOTNULL,
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> typeOperand

            IrTypeOperator.SAFE_CAST -> typeOperand.makeNullable()

            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> builtIns.booleanType
        }

        if (operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT && typeOperand != builtIns.unitType) {
            reportError(expression, "typeOperand is $typeOperand")
        }

        // TODO: check IMPLICIT_NOTNULL's argument type.

        expression.ensureTypeIs(naturalType)
    }

    override fun visitLoop(loop: IrLoop) {
        super.visitLoop(loop)

        loop.ensureTypeIs(builtIns.unitType)
    }

    override fun visitBreakContinue(jump: IrBreakContinue) {
        super.visitBreakContinue(jump)

        jump.ensureTypeIs(builtIns.nothingType)
    }

    override fun visitReturn(expression: IrReturn) {
        super.visitReturn(expression)

        expression.ensureTypeIs(builtIns.nothingType)
    }

    override fun visitThrow(expression: IrThrow) {
        super.visitThrow(expression)

        expression.ensureTypeIs(builtIns.nothingType)
    }
}