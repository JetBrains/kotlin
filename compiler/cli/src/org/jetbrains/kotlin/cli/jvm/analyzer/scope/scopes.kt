/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.analyzer.scope

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.keysToMap


typealias VisitorData = Pair<Boolean, Unit>
typealias Visitor = IrElementVisitor<VisitorData, Unit>

fun falseVisitorData() = false to Unit

abstract class AbstractPredicate {
    abstract val visitor: Visitor
    private val cachedResults = mutableMapOf<IrElement, VisitorData>()
    var info: () -> Unit = {}

    open fun checkIrNode(element: IrElement): VisitorData {
        if (element in cachedResults) {
            return cachedResults[element]!!
        }
        val result = element.accept(visitor, Unit)
        cachedResults[element] = result
        return result
    }
}

abstract class ScopePredicate : AbstractPredicate() {
    protected val innerPredicates = mutableListOf<AbstractPredicate>()

    var recursiveSearch = true

    fun classDefinition(init: ClassPredicate.() -> Unit): ClassPredicate {
        val predicate = ClassPredicate()
        predicate.init()
        innerPredicates += predicate
        return predicate
    }

    fun objectDefinition(init: ObjectPredicate.() -> Unit): ObjectPredicate {
        val predicate = ObjectPredicate()
        predicate.init()
        innerPredicates += predicate
        return predicate
    }

    fun interfaceDefinition(init: InterfacePredicate.() -> Unit): InterfacePredicate {
        val predicate = InterfacePredicate()
        predicate.init()
        innerPredicates += predicate
        return predicate
    }

    fun function(init: FunctionPredicate.() -> Unit): FunctionPredicate {
        val predicate = FunctionPredicate()
        predicate.init()
        innerPredicates += predicate
        return predicate
    }
}

class CodeBlockPredicate : ScopePredicate() {
    override val visitor: Visitor
        get() = MyVisitor()

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: Unit): VisitorData = falseVisitorData()

        override fun visitBlockBody(body: IrBlockBody, data: Unit): VisitorData {
            return visitSmthWithStatements(body, data)
        }

        override fun visitBlock(expression: IrBlock, data: Unit): VisitorData {
            return visitSmthWithStatements(expression, data)
        }

        private fun visitSmthWithStatements(body: IrStatementContainer, data: Unit): VisitorData {
            info()
            val matches = mutableMapOf<AbstractPredicate, Boolean>()
            matches.putAll(innerPredicates.keysToMap { false })
            for (predicate in innerPredicates) {
                for (statement in body.statements) {
                    val (result, map) = predicate.checkIrNode(statement)
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
    }

    fun forLoop(init: ForLoopPredicate.() -> Unit): ForLoopPredicate {
        val predicate = ForLoopPredicate()
        predicate.init()
        innerPredicates += predicate
        return predicate
    }

    fun whileLoop(init: WhileLoopPredicate.() -> Unit): WhileLoopPredicate {
        val predicate = WhileLoopPredicate()
        predicate.init()
        innerPredicates += predicate
        return predicate
    }

    fun ifCondition(init: IfPredicate.() -> Unit): IfPredicate {
        val predicate = IfPredicate()
        predicate.init()
        innerPredicates += predicate
        return predicate
    }

    fun variableDefinition(init: VariablePredicate.() -> Unit): VariablePredicate {
        val predicate = VariablePredicate()
        predicate.init()
        innerPredicates += predicate
        return predicate
    }
    
    fun functionCall(func: FunctionPredicate, init: FunctionCallPredicate.() -> Unit): FunctionCallPredicate {
        val predicate = FunctionCallPredicate(func)
        predicate.init()
        innerPredicates += predicate
        return predicate
    }
    
}

class VariablePredicate : AbstractPredicate() {
    var message: String? = null

    override val visitor: Visitor
        get() = MyVisitor()

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: Unit): VisitorData = falseVisitorData()

        override fun visitVariable(declaration: IrVariable, data: Unit): VisitorData {
            info()
            var s = "variable ${declaration.name}"
            if (message != null) {
                s += ". message: $message"
            }
            println(s)
            return true to Unit
        }
    }
}

class FunctionPredicate : AbstractPredicate() {
    private var body: CodeBlockPredicate? = null

    var name: String? = null

    override val visitor: Visitor
        get() = MyVisitor()

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: Unit): VisitorData = falseVisitorData()

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Unit): VisitorData {
            if (name != null && declaration.name.identifier != name) {
                return falseVisitorData()
            }
            var result = true
            if (body != null && declaration.body != null) {
                result = body?.checkIrNode(declaration.body!!)!!.first
            }
            if (result) {
                info()
            }
            return result to Unit
        }
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

    override val visitor: Visitor
        get() = MyVisitor()

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: Unit): VisitorData = falseVisitorData()

        override fun visitWhen(expression: IrWhen, data: Unit): VisitorData {
            if (expression.branches.size < 2 && elsePredicate != null) {
                return falseVisitorData()
            }
            var thenResult = true
            var elseResult = true
            if (thenPredicate != null) {
                val (result, map) = thenPredicate!!.checkIrNode(expression.branches[0])
                thenResult = result
            }
            if (elsePredicate != null) {
                val (result, map) = elsePredicate!!.checkIrNode(expression.branches[1])
                elseResult = result
            }
            val result = thenResult && elseResult
            if (result) {
                info()
            }
            return result to Unit
        }
    }

    fun thenBranch(init: CodeBlockPredicate.() -> Unit): CodeBlockPredicate {
        val predicate = CodeBlockPredicate()
        predicate.init()
        thenPredicate = predicate
        return predicate
    }

    fun elseBranch(init: CodeBlockPredicate.() -> Unit): CodeBlockPredicate {
        val predicate = CodeBlockPredicate()
        predicate.init()
        elsePredicate = predicate
        return predicate
    }
}

abstract class LoopPredicate : AbstractPredicate() {
    var body: CodeBlockPredicate? = null

    override val visitor: Visitor
        get() = MyVisitor()

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: Unit): VisitorData = falseVisitorData()

        override fun visitBlock(expression: IrBlock, data: Unit): VisitorData {
            if (expression.origin != IrStatementOrigin.FOR_LOOP) {
                return falseVisitorData()
            }

            val whileLoop = expression.statements.firstOrNull { it is IrWhileLoop }
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
    }

    fun body(init: CodeBlockPredicate.() -> Unit): CodeBlockPredicate {
        body = CodeBlockPredicate()
        body?.init()
        return body!!
    }
}

class ForLoopPredicate : LoopPredicate()

class WhileLoopPredicate : LoopPredicate()

class FunctionCallPredicate(val functionPredicate: FunctionPredicate) : AbstractPredicate() {
    override val visitor: Visitor
        get() = MyVisitor()
    
    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: Unit): VisitorData = falseVisitorData()

        override fun visitCall(expression: IrCall, data: Unit): VisitorData {
            val calledFunction = expression.symbol.owner
            return functionPredicate.checkIrNode(calledFunction)
        }
    }
}

class FilePredicate : ScopePredicate() {
    override val visitor: Visitor
        get() = MyVisitor()

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: Unit): VisitorData = falseVisitorData()

        override fun visitFile(declaration: IrFile, data: Unit): VisitorData {
            val matches = mutableMapOf<AbstractPredicate, Boolean>()
            matches.putAll(innerPredicates.keysToMap { false })
            for (predicate in innerPredicates) {
                for (innerDeclaration in declaration.declarations) {
                    val (result, map) = predicate.checkIrNode(innerDeclaration)
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
    TODO: rename predicates to predicates
    в analyzer запихнуть главный предикат, убрать наследование от Scope
    DataHolder = Map<>? / emptyMap
    change recursiveSearch to everywhere {...} *minor
    сделать свой класс для типов, сравнивать типы по fqn
 */