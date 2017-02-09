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
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny

// TODO: synchronize with JVM BE
class Closure(val capturedValues: List<ValueDescriptor>)

abstract class AbstractClosureAnnotator : IrElementVisitorVoid {
    protected abstract fun recordFunctionClosure(functionDescriptor: FunctionDescriptor, closure: Closure)
    protected abstract fun recordClassClosure(classDescriptor: ClassDescriptor, closure: Closure)

    private class ClosureBuilder {
        val capturedValues = mutableSetOf<ValueDescriptor>()
        private val declaredValues = mutableSetOf<ValueDescriptor>()

        fun buildClosure() = Closure(capturedValues.toList())

        fun addNested(closure: Closure) {
            fillInNestedClosure(capturedValues, closure.capturedValues)
        }

        private fun fillInNestedClosure(destination: MutableSet<ValueDescriptor>, nested: List<ValueDescriptor>) {
            nested.filterTo(destination) { isExternal(it) }
        }

        fun declareVariable(valueDescriptor: ValueDescriptor?) {
            if (valueDescriptor != null)
                declaredValues.add(valueDescriptor)
        }

        fun seeVariable(valueDescriptor: ValueDescriptor) {
            if (isExternal(valueDescriptor))
                capturedValues.add(valueDescriptor)
        }

        fun isExternal(valueDescriptor: ValueDescriptor): Boolean {
            return !declaredValues.contains(valueDescriptor)
        }
    }

    private val classClosures = mutableMapOf<ClassDescriptor, Closure>()
    private val closuresStack = mutableListOf<ClosureBuilder>()

    private fun <E> MutableList<E>.push(element: E) = this.add(element)

    private fun <E> MutableList<E>.pop() = this.removeAt(size - 1)

    private fun <E> MutableList<E>.peek(): E? = if (size == 0) null else this[size - 1]

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        val classDescriptor = declaration.descriptor
        val closureBuilder = ClosureBuilder()

        closureBuilder.declareVariable(classDescriptor.thisAsReceiverParameter)
        if (classDescriptor.isInner)
            closureBuilder.declareVariable((classDescriptor.containingDeclaration as ClassDescriptor).thisAsReceiverParameter)

        closuresStack.push(closureBuilder)
        declaration.acceptChildrenVoid(this)
        closuresStack.pop()

        val superClassClosure = classClosures[classDescriptor.getSuperClassOrAny()]
        if (superClassClosure != null) {
            // Capture all values from the super class since we need to call constructor of super class
            // with its captured values.
            closureBuilder.addNested(superClassClosure)
        }

        val closure = closureBuilder.buildClosure()

        if (DescriptorUtils.isLocal(classDescriptor)) {
            recordClassClosure(classDescriptor, closure)
            classClosures[classDescriptor] = closure
        }

        closuresStack.peek()?.addNested(closure)
    }

    override fun visitFunction(declaration: IrFunction) {
        val functionDescriptor = declaration.descriptor
        val closureBuilder = ClosureBuilder()

        functionDescriptor.valueParameters.forEach { closureBuilder.declareVariable(it) }
        closureBuilder.declareVariable(functionDescriptor.dispatchReceiverParameter)
        closureBuilder.declareVariable(functionDescriptor.extensionReceiverParameter)
        if (functionDescriptor is ConstructorDescriptor)
            closureBuilder.declareVariable(functionDescriptor.constructedClass.thisAsReceiverParameter)

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
        closuresStack.peek()?.seeVariable(expression.descriptor)
        super.visitVariableAccess(expression)
    }

    override fun visitVariable(declaration: IrVariable) {
        closuresStack.peek()?.declareVariable(declaration.descriptor)
        super.visitVariable(declaration)
    }
}