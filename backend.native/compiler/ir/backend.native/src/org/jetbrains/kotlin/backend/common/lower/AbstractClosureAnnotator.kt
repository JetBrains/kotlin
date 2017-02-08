package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrSetterCallImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils

// TODO: synchronize with JVM BE
class Closure(val capturedValues: List<ValueDescriptor>)

abstract class AbstractClosureAnnotator : IrElementVisitorVoid {
    protected abstract fun recordFunctionClosure(functionDescriptor: FunctionDescriptor, closure: Closure)
    protected abstract fun recordClassClosure(classDescriptor: ClassDescriptor, closure: Closure)

    private abstract class ClosureBuilder(open val owner: DeclarationDescriptor) {
        val capturedValues = mutableSetOf<ValueDescriptor>()

        fun buildClosure() = Closure(capturedValues.toList())

        fun addNested(closure: Closure) {
            fillInNestedClosure(capturedValues, closure.capturedValues)
        }

        private fun <T : CallableDescriptor> fillInNestedClosure(destination: MutableSet<T>, nested: List<T>) {
            nested.filterTo(destination) {
                isExternal(it)
            }
        }

        abstract fun <T : CallableDescriptor> isExternal(valueDescriptor: T): Boolean
    }

    private class FunctionClosureBuilder(override val owner: FunctionDescriptor) : ClosureBuilder(owner) {

        override fun <T : CallableDescriptor> isExternal(valueDescriptor: T): Boolean =
                valueDescriptor.containingDeclaration != owner && valueDescriptor != owner.dispatchReceiverParameter
    }

    private class ClassClosureBuilder(override val owner: ClassDescriptor) : ClosureBuilder(owner) {

        override fun <T : CallableDescriptor> isExternal(valueDescriptor: T): Boolean {
            // TODO: replace with 'return valueDescriptor.containingDeclaration != owner' after constructors lowering.
            var declaration: DeclarationDescriptor? = valueDescriptor.containingDeclaration
            while (declaration != null && declaration != owner) {
                declaration = declaration.containingDeclaration
            }
            return declaration != owner
        }

    }

    private val closuresStack = mutableListOf<ClosureBuilder>()

    private fun <E> MutableList<E>.push(element: E) = this.add(element)

    private fun <E> MutableList<E>.pop() = this.removeAt(size - 1)

    private fun <E> MutableList<E>.peek(): E? = if (size == 0) null else this[size - 1]

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        val classDescriptor = declaration.descriptor
        val closureBuilder = ClassClosureBuilder(classDescriptor)

        closuresStack.push(closureBuilder)
        declaration.acceptChildrenVoid(this)
        closuresStack.pop()

        val closure = closureBuilder.buildClosure()

        if (DescriptorUtils.isLocal(classDescriptor)) {
            recordClassClosure(classDescriptor, closure)
        }

        closuresStack.peek()?.addNested(closure)
    }

    // TODO: remove as soon as bug in IrSetterCallImpl is fixed.
    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)
        if (expression is IrSetterCallImpl)
            visitElement(expression.getValueArgument(0)!!)
    }

    override fun visitFunction(declaration: IrFunction) {
        val functionDescriptor = declaration.descriptor
        val closureBuilder = FunctionClosureBuilder(functionDescriptor)

        closuresStack.push(closureBuilder)
        declaration.acceptChildrenVoid(this)
        closuresStack.pop()

        val closure = closureBuilder.buildClosure()

        if (DescriptorUtils.isLocal(functionDescriptor)) {
            recordFunctionClosure(functionDescriptor, closure)
        }

        closuresStack.peek()?.addNested(closure)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
        // Getter and setter of local delegated properties are special generated functions and don't have closure.
        declaration.delegate.initializer?.acceptVoid(this)
    }

    override fun visitVariableAccess(expression: IrValueAccessExpression) {
        val closureBuilder = closuresStack.peek()

        if (closureBuilder != null) {
            val variableDescriptor = expression.descriptor
            if (closureBuilder.isExternal(variableDescriptor)) {
                closureBuilder.capturedValues.add(variableDescriptor)
            }
        }

        expression.acceptChildrenVoid(this)
    }

}