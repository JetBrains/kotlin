package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny

// TODO: synchronize with JVM BE
class Closure(val capturedValues: List<ValueDescriptor>)

private fun <E> MutableList<E>.push(element: E) = this.add(element)

private fun <E> MutableList<E>.pop() = this.removeAt(size - 1)

private fun <E> MutableList<E>.peek(): E? = if (size == 0) null else this[size - 1]

abstract class AbstractClosureAnnotator  {
    protected abstract fun recordFunctionClosure(functionDescriptor: FunctionDescriptor, closure: Closure)
    protected abstract fun recordClassClosure(classDescriptor: ClassDescriptor, closure: Closure)

    private class ClosureBuilder {
        val capturedValues = mutableSetOf<ValueDescriptor>()
        private val nestedBuilders = mutableSetOf<ClosureBuilder>()
        private val declaredValues = mutableSetOf<ValueDescriptor>()

        fun buildClosure() : Closure {
            val processed = mutableSetOf<ClosureBuilder>(this)
            val builderStack = mutableListOf<ClosureBuilder>().apply { addAll(nestedBuilders) }
            while (builderStack.isNotEmpty()) {
                val builder = builderStack.pop()
                if (!processed.contains(builder)) {
                    processed.add(builder)
                    builderStack.addAll(builder.nestedBuilders)
                    fillInNestedClosure(capturedValues, builder.capturedValues)
                }
            }
            // TODO: Save the closure and reuse it.
            return Closure(capturedValues.toList())
        }

        fun addNested(builder: ClosureBuilder) {
            nestedBuilders.add(builder)
            declaredValues.addAll(builder.declaredValues)
        }

        private fun fillInNestedClosure(destination: MutableSet<ValueDescriptor>, nested: Collection<ValueDescriptor>) {
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

    private val closureBuilders = mutableMapOf<DeclarationDescriptor, ClosureBuilder>()

    fun annotate(declaration: IrDeclaration) {
        // First pass - collect all closures for classes and functions. Collect call graph
        declaration.acceptChildrenVoid(ClosureCollectorVisitor())
        // Second pass - build closures on basis of calls.
        closureBuilders.forEach { descriptor, builder ->
            when(descriptor) {
                is FunctionDescriptor -> recordFunctionClosure(descriptor, builder.buildClosure())
                is ClassDescriptor -> recordClassClosure(descriptor, builder.buildClosure())
                else -> throw AssertionError("Unexpected descriptor type.")
            }
        }
    }

    private inner class ClosureCollectorVisitor : IrElementVisitorVoid {

        protected val closuresStack = mutableListOf<ClosureBuilder>()
        protected val classClosures = mutableMapOf<ClassDescriptor, ClosureBuilder>()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            val classDescriptor = declaration.descriptor
            val closureBuilder = ClosureBuilder()
            closureBuilders[declaration.descriptor] = closureBuilder

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

            if (DescriptorUtils.isLocal(classDescriptor)) {
                classClosures[classDescriptor] = closureBuilder
            }

            closuresStack.peek()?.addNested(closureBuilder)
        }

        override fun visitFunction(declaration: IrFunction) {
            val functionDescriptor = declaration.descriptor
            val closureBuilder = ClosureBuilder()
            closureBuilders[declaration.descriptor] = closureBuilder

            functionDescriptor.valueParameters.forEach { closureBuilder.declareVariable(it) }
            closureBuilder.declareVariable(functionDescriptor.dispatchReceiverParameter)
            closureBuilder.declareVariable(functionDescriptor.extensionReceiverParameter)
            if (functionDescriptor is ConstructorDescriptor)
                closureBuilder.declareVariable(functionDescriptor.constructedClass.thisAsReceiverParameter)

            closuresStack.push(closureBuilder)
            declaration.acceptChildrenVoid(this)
            closuresStack.pop()

            closuresStack.peek()?.addNested(closureBuilder)
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

        override fun visitCall(expression: IrCall) {
            expression.acceptChildrenVoid(this)
            val descriptor = expression.descriptor
            if (DescriptorUtils.isLocal(descriptor)) {
                val builder = closureBuilders[descriptor]
                builder?.let {
                    closuresStack.peek()?.addNested(builder)
                }
            }
        }
    }
}