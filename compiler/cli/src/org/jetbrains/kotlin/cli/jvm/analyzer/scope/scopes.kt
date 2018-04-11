/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.analyzer.scope

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.keysToMap


typealias VisitorData = Pair<Boolean, Unit>
typealias Visitor = IrElementVisitor<VisitorData, Unit>

fun falseVisitorData() = false to Unit

abstract class AbstractPredicate {
//    abstract val visitor: Visitor
    var info: () -> Unit = {}

    abstract fun checkIrNode(element: IrElement): VisitorData
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
    override fun checkIrNode(element: IrElement): VisitorData {
        if (element !is IrStatementContainer) {
            return falseVisitorData()
        }
        info()
        val matches = mutableMapOf<AbstractPredicate, Boolean>()
        matches.putAll(innerPredicates.keysToMap { false })
        for (predicate in innerPredicates) {
            for (statement in element.statements) {
                val (result, data) = predicate.checkIrNode(statement)
                if (result) {
                    matches[predicate] = true
                }
            }
        }
        if (matches.values.all{ it }) {
            return true to Unit
        } else {
            return falseVisitorData()
        }
    }

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
}

class VariablePredicate : AbstractPredicate() {
    var message: String? = null

    override fun checkIrNode(element: IrElement): VisitorData {
        if (element !is IrVariable) {
            return falseVisitorData()
        }
        info()
        var s = "variable ${element.name}"
        if (message != null) {
            s += ". message: $message"
        }
        println(s)
        return true to Unit
    }
}

class FunctionPredicate : AbstractPredicate() {
    private var body: CodeBlockPredicate? = null

    var name: String? = null

    override fun checkIrNode(element: IrElement): VisitorData {
        if (element !is IrFunction) {
            return falseVisitorData()
        }

        if (name != null && element is IrSimpleFunction && element.name.identifier != name) {
            return falseVisitorData()
        }
        var result = true
        if (body != null && element.body != null) {
            result = body?.checkIrNode(element.body!!)!!.first
        }
        if (result) {
            info()
        }
        return true to Unit
    }

    fun body(init: CodeBlockPredicate.() -> Unit): CodeBlockPredicate {
        body = CodeBlockPredicate()
        body?.init()
        return body!!
    }
}

class IfPredicate : AbstractPredicate() {
    private var thenPredicate: CodeBlockPredicate? = null
    private var elsePredicate: CodeBlockPredicate? = null

    override fun checkIrNode(element: IrElement): VisitorData {
        if (element !is IrIfThenElseImpl) {
            return falseVisitorData()
        }
        if (element.branches.size < 2 && elsePredicate != null) {
            return falseVisitorData()
        }
        var thenResult = true
        var elseResult = true
        if (thenPredicate != null) {
            val (result, data) = thenPredicate!!.checkIrNode(element.branches[0])
            thenResult = result
        }
        if (elsePredicate != null) {
            val (result, data) = elsePredicate!!.checkIrNode(element.branches[1])
            elseResult = result
        }
        val result = thenResult && elseResult
        if (result) {
            info()
        }
        return result to Unit
    }

    fun thenBranch(init: CodeBlockPredicate.() -> Unit): CodeBlockPredicate {
        val scope = CodeBlockPredicate()
        scope.init()
        thenPredicate = scope
        return scope
    }

    fun elseBranch(init: CodeBlockPredicate.() -> Unit): CodeBlockPredicate {
        val scope = CodeBlockPredicate()
        scope.init()
        elsePredicate = scope
        return scope
    }
}

abstract class LoopPredicate : AbstractPredicate() {
    var body: CodeBlockPredicate? = null

    override fun checkIrNode(element: IrElement): VisitorData {
        if (element !is IrBlock || element.origin != IrStatementOrigin.FOR_LOOP) {
            return falseVisitorData()
        }

        val whileLoop = element.statements.firstOrNull { it is IrWhileLoop }
        if (whileLoop != null && body != null) {
            val loopBody = (whileLoop as IrWhileLoop).body ?: return falseVisitorData()
            if (loopBody is IrBlock && loopBody.statements.size >= 2) {
                info()
                return body!!.checkIrNode(loopBody.statements[1])
            }
        }
        info()
        return true to Unit
    }

    fun body(init: CodeBlockPredicate.() -> Unit): CodeBlockPredicate {
        body = CodeBlockPredicate()
        body?.init()
        return body!!
    }
}

class ForLoopPredicate : LoopPredicate()

class WhileLoopPredicate : LoopPredicate()

class FunctionCallPredicate : AbstractPredicate() {
    override fun checkIrNode(element: IrElement): VisitorData {
        TODO("not implemented")
    }
}

class FilePredicate : ScopePredicate() {
    override fun checkIrNode(element: IrElement): VisitorData {
        if (element !is IrFile) {
            return falseVisitorData()
        }
        val matches = mutableMapOf<AbstractPredicate, Boolean>()
        matches.putAll(innerPredicates.keysToMap { false })
        for (predicate in innerPredicates) {
            for (declaration in element.declarations) {
                val (result, data) = predicate.checkIrNode(declaration)
                if (result) {
                    matches[predicate] = true
                }
            }
        }
        if (matches.values.all{ it }) {
            info()
            return true to Unit
        } else {
            return falseVisitorData()
        }
    }
}

class Analyzer(
    val title: String,
    val predicate: AbstractPredicate
) {
    fun execute(irModule: IrModuleFragment, moduleDescriptor: ModuleDescriptor, bindingContext: BindingContext){
        for (file in irModule.files) {
            val (result, data) = predicate.checkIrNode(file)
            println("${file.fqName}: predicate is ${result}")
        }
    }
}

fun analyzer(title: String, init: FilePredicate.() -> Unit): Analyzer {
    val predicate = FilePredicate()
    predicate.init()
    return Analyzer(title, predicate)
}

/*
    TODO: rename scopes to predicates
    в analyzer запихнуть главный предикат, убрать наследование от Scope
    DataHolder = Map<>? / emptyMap
    change recursiveSearch to everywhere {...} *minor
    сделать свой класс для типов, сравнивать типы по fqn
 */