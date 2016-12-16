package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType


/**
 * Transforms expressions depending on the context they are used in.
 *
 * The transformations are defined with `IrExpression.use*` methods in this class,
 * the most common are [useAs], [useAsStatement], [useInTypeOperator].
 *
 * TODO: the implementation is originally based on [org.jetbrains.kotlin.psi2ir.transformations.InsertImplicitCasts]
 * and should probably be used as its base.
 *
 * TODO: consider making this visitor non-recursive to make it more general.
 */
abstract class AbstractValueUsageTransformer(val builtIns: KotlinBuiltIns): IrElementTransformerVoid() {

    protected open fun IrExpression.useAs(type: KotlinType): IrExpression = this

    protected open fun IrExpression.useAsStatement(): IrExpression = this

    protected open fun IrExpression.useInTypeOperator(operator: IrTypeOperator, typeOperand: KotlinType): IrExpression =
            this

    protected open fun IrExpression.useAsValue(value: ValueDescriptor): IrExpression = this.useAs(value.type)

    protected open fun IrExpression.useAsArgument(parameter: ParameterDescriptor): IrExpression =
            this.useAsValue(parameter)

    protected open fun IrExpression.useForVariable(variable: VariableDescriptor): IrExpression =
            this.useAsValue(variable)

    protected open fun IrExpression.useForField(field: PropertyDescriptor): IrExpression =
            this.useForVariable(field)

    protected open fun IrExpression.useAsReturnValue(returnTarget: CallableDescriptor): IrExpression {
        val returnType = returnTarget.returnType ?: return this
        return this.useAs(returnType)
    }

    protected open fun IrExpression.useAsResult(enclosing: IrExpression): IrExpression =
            this.useAs(enclosing.type)

    override fun visitMemberAccess(expression: IrMemberAccessExpression): IrExpression {
        expression.transformChildrenVoid(this)

        with(expression) {
            dispatchReceiver = dispatchReceiver?.useAsArgument(descriptor.dispatchReceiverParameter!!)
            extensionReceiver = extensionReceiver?.useAsArgument(descriptor.extensionReceiverParameter!!)
            for (index in descriptor.valueParameters.indices) {
                val argument = getValueArgument(index) ?: continue
                val parameter = descriptor.valueParameters[index]
                putValueArgument(index, argument.useAsArgument(parameter))
            }
        }

        return expression
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {
        body.transformChildrenVoid(this)

        body.statements.forEachIndexed { i, irStatement ->
            if (irStatement is IrExpression) {
                body.statements[i] = irStatement.useAsStatement()
            }
        }

        return body
    }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        expression.transformChildrenVoid(this)

        if (expression.statements.isEmpty()) {
            return expression
        }

        val lastIndex = expression.statements.lastIndex
        expression.statements.forEachIndexed { i, irStatement ->
            if (irStatement is IrExpression) {
                expression.statements[i] =
                        if (i == lastIndex)
                            irStatement.useAsResult(expression)
                        else
                            irStatement.useAsStatement()
            }
        }

        return expression
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        expression.transformChildrenVoid(this)

        expression.value = expression.value.useAsReturnValue(expression.returnTarget)

        return expression
    }

    override fun visitSetVariable(expression: IrSetVariable): IrExpression {
        expression.transformChildrenVoid(this)

        expression.value = expression.value.useForVariable(expression.descriptor)

        return expression
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        expression.transformChildrenVoid(this)

        expression.value = expression.value.useForField(expression.descriptor)

        return expression
    }

    override fun visitField(declaration: IrField): IrStatement {
        declaration.transformChildrenVoid(this)

        declaration.initializer?.let {
            it.expression = it.expression.useForField(declaration.descriptor)
        }

        return declaration
    }

    override fun visitVariable(declaration: IrVariable): IrVariable {
        declaration.transformChildrenVoid(this)

        declaration.initializer = declaration.initializer?.useForVariable(declaration.descriptor)

        return declaration
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        expression.transformChildrenVoid(this)

        for (irBranch in expression.branches) {
            irBranch.condition = irBranch.condition.useAs(builtIns.booleanType)
            irBranch.result = irBranch.result.useAsResult(expression)
        }

        return expression
    }

    override fun visitLoop(loop: IrLoop): IrExpression {
        loop.transformChildrenVoid(this)

        loop.condition = loop.condition.useAs(builtIns.booleanType)

        loop.body = loop.body?.useAsStatement()

        return loop
    }

    override fun visitThrow(expression: IrThrow): IrExpression {
        expression.transformChildrenVoid(this)

        expression.value = expression.value.useAs(builtIns.throwable.defaultType)

        return expression
    }

    override fun visitTry(aTry: IrTry): IrExpression {
        aTry.transformChildrenVoid(this)

        aTry.tryResult = aTry.tryResult.useAsResult(aTry)

        for (aCatch in aTry.catches) {
            aCatch.result = aCatch.result.useAsResult(aTry)
        }

        aTry.finallyExpression = aTry.finallyExpression?.useAsStatement()

        return aTry
    }

    override fun visitVararg(expression: IrVararg): IrExpression {
        expression.transformChildrenVoid(this)

        expression.elements.forEachIndexed { i, element ->
            when (element) {
                is IrSpreadElement ->
                    element.expression = element.expression.useAs(expression.type)
                is IrExpression ->
                    expression.putElement(i, element.useAs(expression.varargElementType))
            }
        }

        return expression
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)

        expression.argument = expression.argument.useInTypeOperator(expression.operator, expression.typeOperand)

        return expression
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        declaration.transformChildrenVoid(this)

        declaration.descriptor.valueParameters.forEach { parameter ->
            val defaultValue = declaration.getDefault(parameter)
            if (defaultValue is IrExpressionBody) {
                defaultValue.expression = defaultValue.expression.useAsArgument(parameter)
            }
        }

        declaration.body?.let {
            if (it is IrExpressionBody) {
                it.expression = it.expression.useAsReturnValue(declaration.descriptor)
            }
        }

        return declaration
    }

    // TODO: IrStringConcatenation, IrEnumEntry?

}

