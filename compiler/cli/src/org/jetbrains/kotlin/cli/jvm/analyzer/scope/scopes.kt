/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.analyzer.scope

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.BindingContext

abstract class AnalyzerComponent {
    abstract val visitor: IrElementVisitorVoid
    var info: () -> Unit = {}

    abstract fun checkIrNode(element: IrElement): Boolean
}

abstract class AbstractScope : AnalyzerComponent() {
    protected val innerScopes = mutableListOf<AnalyzerComponent>()

    var recursiveSearch = true

    fun classDefinition(init: ClassScope.() -> Unit): ClassScope {
        val scope = ClassScope()
        scope.init()
        innerScopes += scope
        return scope
    }

    fun objectDefinition(init: ObjectScope.() -> Unit): ObjectScope {
        val scope = ObjectScope()
        scope.init()
        innerScopes += scope
        return scope
    }

    fun interfaceDefinition(init: InterfaceScope.() -> Unit): InterfaceScope {
        val scope = InterfaceScope()
        scope.init()
        innerScopes += scope
        return scope
    }

    fun function(init: FunctionDefinition.() -> Unit): FunctionDefinition {
        val scope = FunctionDefinition()
        scope.init()
        innerScopes += scope
        return scope
    }
}

class CodeScope : AbstractScope() {
    override fun checkIrNode(element: IrElement): Boolean = true

    fun forLoop(init: ForLoop.() -> Unit): ForLoop {
        val scope = ForLoop()
        scope.init()
        innerScopes += scope
        return scope
    }

    fun whileLoop(init: WhileLoop.() -> Unit): WhileLoop {
        val scope = WhileLoop()
        scope.init()
        innerScopes += scope
        return scope
    }

    fun ifCondition(init: IfCondition.() -> Unit): IfCondition {
        val scope = IfCondition()
        scope.init()
        innerScopes += scope
        return scope
    }

    fun variableDefinition(init: VariableDefinition.() -> Unit): VariableDefinition {
        val scope = VariableDefinition()
        scope.init()
        innerScopes += scope
        return scope
    }

    override val visitor: IrElementVisitorVoid = MyVisitor()

    inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            innerScopes.forEach { element.acceptVoid(it.visitor) }
            if (recursiveSearch) {
                element.acceptChildrenVoid(this)
            }
        }
    }
}

open class ClassScope : AbstractScope() {
    override val visitor: IrElementVisitorVoid
        get() = TODO("not implemented")

    override fun checkIrNode(element: IrElement): Boolean {
        TODO("not implemented")
    }

    fun propertyDefinition(init: PropertyDefinition.() -> Unit): PropertyDefinition {
        val scope = PropertyDefinition()
        scope.init()
        innerScopes += scope
        return scope
    }
}

class ObjectScope : ClassScope()

class InterfaceScope : ClassScope()

class FunctionDefinition : AnalyzerComponent() {
    override val visitor: IrElementVisitorVoid = MyVisitor()

    private var body: CodeScope? = null

    var name: String? = null

    override fun checkIrNode(element: IrElement): Boolean {
        if (element !is IrFunction) {
            return false
        }
        if (name != null && element is IrSimpleFunction) {
            return element.name.identifier == name
        }
        return true
    }

    fun body(init: CodeScope.() -> Unit): CodeScope {
        body = CodeScope()
        body?.init()
        return body!!
    }

    inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {}

        override fun visitFunction(declaration: IrFunction) {
            if (!checkIrNode(declaration)) {
                return
            }
            info()
            if (body != null) {
                declaration.body?.acceptChildrenVoid(body!!.visitor)
            }
        }
    }
}

class VariableDefinition() : AnalyzerComponent() {
    override val visitor: IrElementVisitorVoid = MyVisitor()

    var message: String? = null

    override fun checkIrNode(element: IrElement): Boolean {
        // TODO
        return true
    }

    inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {}

        override fun visitVariable(declaration: IrVariable) {
            if (!checkIrNode(declaration)) {
                return
            }

            var s = "variable ${declaration.name}"
            if (message != null) {
                s += ". message: $message"
            }
            println(s)
        }
    }
}

class PropertyDefinition : AnalyzerComponent() {
    override val visitor: IrElementVisitorVoid = MyVisitor()

    override fun checkIrNode(element: IrElement): Boolean {
        TODO("not implemented")
    }

    inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            TODO("not implemented")
        }
    }
}

class IfCondition : AnalyzerComponent() {
    override val visitor: IrElementVisitorVoid = MyVisitor()

    private val thenScopes = mutableListOf<CodeScope>()
    private val elseScopes = mutableListOf<CodeScope>()

    override fun checkIrNode(element: IrElement): Boolean {
        // TODO
        return true
    }

    fun thenBranch(init: CodeScope.() -> Unit): CodeScope {
        val scope = CodeScope()
        scope.init()
        thenScopes += scope
        return scope
    }

    fun elseBranch(init: CodeScope.() -> Unit): CodeScope {
        val scope = CodeScope()
        scope.init()
        elseScopes += scope
        return scope
    }

    inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {}

        override fun visitWhen(expression: IrWhen) {
            if (expression is IrIfThenElseImpl) {
                if (!checkIrNode(expression)) {
                    return
                }
                info()
                thenScopes.forEach { expression.branches[0].acceptVoid(it.visitor) }
                if (expression.branches.size > 1) {
                    elseScopes.forEach { expression.branches[1].acceptVoid(it.visitor) }
                }
            }
        }
    }
}

class ForLoop : AnalyzerComponent() {
    override val visitor: IrElementVisitorVoid = MyVisitor()

    private var body: CodeScope? = null

    override fun checkIrNode(element: IrElement): Boolean {
        // TODO
        return true
    }

    fun body(init: CodeScope.() -> Unit): CodeScope {
        body = CodeScope()
        body?.init()
        return body!!
    }

    inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {}

        override fun visitBlock(expression: IrBlock) {
            if (expression.origin == IrStatementOrigin.FOR_LOOP && checkIrNode(expression)) {
                info()
                val whileLoop = expression.statements.firstOrNull { it is IrWhileLoop }
                if (whileLoop != null && body != null) {
                    val loopBody = (whileLoop as IrWhileLoop).body ?: return
                    if (loopBody is IrBlock && loopBody.statements.size >= 2) {
                        loopBody.statements[1].acceptChildrenVoid(body!!.visitor)
                    }
                }
            }
        }
    }
}

class WhileLoop : AbstractScope() {
    override val visitor: IrElementVisitorVoid = MyVisitor()

    override fun checkIrNode(element: IrElement): Boolean {
        // TODO
        return true
    }

    inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {}

        override fun visitWhileLoop(loop: IrWhileLoop) {
            if (!checkIrNode(loop)) {
                return
            }
            info()
            innerScopes.forEach { loop.body?.acceptChildrenVoid(it.visitor) }
        }
    }

}

class NewAnalyzer : AbstractScope() {
    fun execute(irModule: IrModuleFragment, moduleDescriptor: ModuleDescriptor, bindingContext: BindingContext) {
        irModule.acceptChildrenVoid(visitor)
    }

    override fun checkIrNode(element: IrElement): Boolean = true

    override val visitor: IrElementVisitorVoid = MyVisitor()

    var title: String = ""

    inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFile(declaration: IrFile) {
            innerScopes.forEach { declaration.acceptChildrenVoid(it.visitor) }
        }
    }
}

fun newAnalyzer(init: NewAnalyzer.() -> Unit): NewAnalyzer {
    val analyzer = NewAnalyzer()
    analyzer.init()
    return analyzer
}
