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


typealias VisitorData = BooleanHolder
typealias Visitor = IrElementVisitor<Unit, VisitorData>

abstract class AbstractPredicate {
    abstract val visitor: Visitor
    var info: () -> Unit = {}

    abstract fun checkIrNode(element: IrElement): Boolean
}

abstract class ScopePredicate : AbstractPredicate() {
    protected val innerPredicates = mutableListOf<AbstractPredicate>()

    var recursiveSearch = true

    fun classDefinition(init: ClassPredicate.() -> Unit): ClassPredicate {
        val scope = ClassPredicate()
        scope.init()
        innerPredicates += scope
        return scope
    }

    fun objectDefinition(init: ObjectPredicate.() -> Unit): ObjectPredicate {
        val scope = ObjectPredicate()
        scope.init()
        innerPredicates += scope
        return scope
    }

    fun interfaceDefinition(init: InterfacePredicate.() -> Unit): InterfacePredicate {
        val scope = InterfacePredicate()
        scope.init()
        innerPredicates += scope
        return scope
    }

    fun function(init: FunctionPredicate.() -> Unit): FunctionPredicate {
        val scope = FunctionPredicate()
        scope.init()
        innerPredicates += scope
        return scope
    }
}

class CodeBlockPredicate : ScopePredicate() {
    override fun checkIrNode(element: IrElement): Boolean = true

    fun forLoop(init: ForLoopPredicate.() -> Unit): ForLoopPredicate {
        val scope = ForLoopPredicate()
        scope.init()
        innerPredicates += scope
        return scope
    }

    fun whileLoop(init: WhileLoopPredicate.() -> Unit): WhileLoopPredicate {
        val scope = WhileLoopPredicate()
        scope.init()
        innerPredicates += scope
        return scope
    }

    fun ifCondition(init: IfPredicate.() -> Unit): IfPredicate {
        val scope = IfPredicate()
        scope.init()
        innerPredicates += scope
        return scope
    }

    fun variableDefinition(init: VariablePredicate.() -> Unit): VariablePredicate {
        val scope = VariablePredicate()
        scope.init()
        innerPredicates += scope
        return scope
    }

    override val visitor: Visitor = MyVisitor()

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: VisitorData) {
            innerPredicates.forEach { element.accept(it.visitor, data) }
            if (recursiveSearch) {
                element.acceptChildren(this, data)
            }
        }
    }
}

class VariablePredicate : AbstractPredicate() {
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

class FunctionPredicate : AbstractPredicate() {
    override val visitor: Visitor = MyVisitor()

    private var body: CodeBlockPredicate? = null

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

    fun body(init: CodeBlockPredicate.() -> Unit): CodeBlockPredicate {
        body = CodeBlockPredicate()
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

class IfPredicate : AbstractPredicate() {
    override val visitor: Visitor = MyVisitor()

    private val thenPredicates = mutableListOf<CodeBlockPredicate>()
    private val elsePredicates = mutableListOf<CodeBlockPredicate>()

    override fun checkIrNode(element: IrElement): Boolean {
        // TODO
        return true
    }

    fun thenBranch(init: CodeBlockPredicate.() -> Unit): CodeBlockPredicate {
        val scope = CodeBlockPredicate()
        scope.init()
        thenPredicates += scope
        return scope
    }

    fun elseBranch(init: CodeBlockPredicate.() -> Unit): CodeBlockPredicate {
        val scope = CodeBlockPredicate()
        scope.init()
        elsePredicates += scope
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
                thenPredicates.forEach { expression.branches[0].accept(it.visitor, data) }
                if (expression.branches.size > 1) {
                    elsePredicates.forEach { expression.branches[1].accept(it.visitor, data) }
                }
            }
        }
    }
}

class ForLoopPredicate : AbstractPredicate() {
    override val visitor: Visitor = MyVisitor()

    private var body: CodeBlockPredicate? = null

    override fun checkIrNode(element: IrElement): Boolean {
        // TODO
        return true
    }

    fun body(init: CodeBlockPredicate.() -> Unit): CodeBlockPredicate {
        body = CodeBlockPredicate()
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

class WhileLoopPredicate : ScopePredicate() {
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
            innerPredicates.forEach { loop.body?.acceptChildren(it.visitor, data) }
        }
    }

}

class FunctionCallPredicate : AbstractPredicate() {
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

class NewAnalyzer : ScopePredicate() {
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
            innerPredicates.forEach { declaration.acceptChildren(it.visitor, data) }
        }
    }
}

fun newAnalyzer(init: NewAnalyzer.() -> Unit): NewAnalyzer {
    val analyzer = NewAnalyzer()
    analyzer.init()
    return analyzer
}

/*
    TODO: rename scopes to predicates
    в analyzer запихнуть главный предикат, убрать наследование от Scope
    DataHolder = Map<>? / emptyMap
    change recursiveSearch to everywhere {...} *minor
    сделать свой класс для типов, сравнивать типы по fqn
 */