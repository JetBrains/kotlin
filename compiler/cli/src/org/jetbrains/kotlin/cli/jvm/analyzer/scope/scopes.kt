/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.analyzer.scope

import org.jetbrains.kotlin.cli.jvm.analyzer.BooleanHolder
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.BindingContext


typealias Visitor = IrElementVisitor<Unit, BooleanHolder>
typealias VisitorData = BooleanHolder

abstract class AnalyzerComponent {
    abstract val visitor: Visitor
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

    override val visitor: Visitor = MyVisitor()

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: VisitorData) {
            innerScopes.forEach { element.accept(it.visitor, data) }
            if (recursiveSearch) {
                element.acceptChildren(this, data)
            }
        }
    }
}

class VariableDefinition() : AnalyzerComponent() {
    override val visitor: Visitor = MyVisitor()

    var message: String? = null

    override fun checkIrNode(element: IrElement): Boolean {
        // TODO
        return true
    }

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: VisitorData) {}

        override fun visitVariable(declaration: IrVariable, data: VisitorData) {
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

class FunctionDefinition : AnalyzerComponent() {
    override val visitor: Visitor = MyVisitor()

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

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: VisitorData) {}

        override fun visitFunction(declaration: IrFunction, data: VisitorData) {
            if (!checkIrNode(declaration)) {
                return
            }
            info()
            if (body != null) {
                declaration.body?.acceptChildren(body!!.visitor, data)
            }
        }
    }
}

class IfCondition : AnalyzerComponent() {
    override val visitor: Visitor = MyVisitor()

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

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: VisitorData) {}

        override fun visitWhen(expression: IrWhen, data: VisitorData) {
            if (expression is IrIfThenElseImpl) {
                if (!checkIrNode(expression)) {
                    return
                }
                info()
                thenScopes.forEach { expression.branches[0].accept(it.visitor, data) }
                if (expression.branches.size > 1) {
                    elseScopes.forEach { expression.branches[1].accept(it.visitor, data) }
                }
            }
        }
    }
}

class ForLoop : AnalyzerComponent() {
    override val visitor: Visitor = MyVisitor()

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

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: VisitorData) {}

        override fun visitBlock(expression: IrBlock, data: VisitorData) {
            if (expression.origin != IrStatementOrigin.FOR_LOOP || !checkIrNode(expression)) {
                return
            }
            info()
            val whileLoop = expression.statements.firstOrNull { it is IrWhileLoop }
            if (whileLoop != null && body != null) {
                val loopBody = (whileLoop as IrWhileLoop).body ?: return
                if (loopBody is IrBlock && loopBody.statements.size >= 2) {
                    loopBody.statements[1].acceptChildren(body!!.visitor, data)
                }
            }

        }
    }
}

class WhileLoop : AbstractScope() {
    override val visitor: Visitor = MyVisitor()

    override fun checkIrNode(element: IrElement): Boolean {
        // TODO
        return true
    }

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: VisitorData) {}

        override fun visitWhileLoop(loop: IrWhileLoop, data: VisitorData) {
            if (!checkIrNode(loop)) {
                return
            }
            info()
            innerScopes.forEach { loop.body?.acceptChildren(it.visitor, data) }
        }
    }

}

class FunctionCall : AnalyzerComponent() {
    override val visitor: Visitor = MyVisitor()

    override fun checkIrNode(element: IrElement): Boolean {
        TODO("not implemented")
    }

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: VisitorData) {}

        override fun visitCall(expression: IrCall, data: VisitorData) {
            TODO()
        }
    }
}

class NewAnalyzer : AbstractScope() {
    fun execute(irModule: IrModuleFragment, moduleDescriptor: ModuleDescriptor, bindingContext: BindingContext) {
        irModule.acceptChildren(visitor, result)
    }

    override fun checkIrNode(element: IrElement): Boolean = true

    override val visitor: Visitor = MyVisitor()

    val result = VisitorData(false)

    var title: String = ""

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: VisitorData) {
            element.acceptChildren(this, data)
        }

        override fun visitFile(declaration: IrFile, data: VisitorData) {
            innerScopes.forEach { declaration.acceptChildren(it.visitor, data) }
        }
    }
}

fun newAnalyzer(init: NewAnalyzer.() -> Unit): NewAnalyzer {
    val analyzer = NewAnalyzer()
    analyzer.init()
    return analyzer
}
