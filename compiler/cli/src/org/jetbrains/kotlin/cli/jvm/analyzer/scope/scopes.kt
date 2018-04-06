/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.analyzer.scope

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.BindingContext

interface AnalyzerComponent {
    fun execute(
        irModule: IrModuleFragment,
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext
    ) {
        irModule.acceptVoid(visitor)
    }
    
    val visitor: IrElementVisitorVoid
}

open class BaseIrVisitor : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }
}

abstract class AbstractScope : AnalyzerComponent {
    protected val innerScopes = mutableListOf<AnalyzerComponent>()

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

    fun functionScope(init: FunctionDefinition.() -> Unit): FunctionDefinition {
        val scope = FunctionDefinition()
        scope.init()
        innerScopes += scope
        return scope
    }
}

abstract class ScopeWithCallings : AbstractScope() {
    fun variableDefinition(init: VariableDefinition.() -> Unit): VariableDefinition {
        val scope = VariableDefinition()
        scope.init()
        innerScopes += scope
        return scope
    }
}

abstract class CodeScope : ScopeWithCallings() {
    fun forCycle(init: ForCycle.() -> Unit): ForCycle {
        val scope = ForCycle()
        scope.init()
        innerScopes += scope
        return scope
    }

    fun whileCycle(init: WhileCycle.() -> Unit): WhileCycle {
        val scope = WhileCycle()
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
}

open class ClassScope : ScopeWithCallings() {
    override val visitor: IrElementVisitorVoid
        get() = TODO("not implemented")

    fun propertyDefinition(init: PropertyDefinition.() -> Unit): PropertyDefinition {
        val scope = PropertyDefinition()
        scope.init()
        innerScopes += scope
        return scope
    }
}

class ObjectScope : ClassScope()

class InterfaceScope : ClassScope()

class FunctionDefinition : ScopeWithCallings() {
    override val visitor: IrElementVisitorVoid = MyVisitor()

    private inner class MyVisitor : BaseIrVisitor() {
        override fun visitElement(element: IrElement) {}

        override fun visitFunction(declaration: IrFunction) {
            TODO()
        }
    }
}

class FunctionBody : CodeScope() {
    override val visitor: IrElementVisitorVoid = MyVisitor()

    private inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
//            innerScopes.forEach { element.acceptChildrenVoid(it.visitor) }
        }

        override fun visitFunction(declaration: IrFunction) {
            innerScopes.forEach { declaration.body?.acceptChildrenVoid(it.visitor) }
        }
    }
}

class VariableDefinition : AnalyzerComponent {
    override val visitor: IrElementVisitorVoid = MyVisitor()

    private inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitVariable(declaration: IrVariable) {
            println("variable ${declaration.name}")
        }
    }
}

class PropertyDefinition : AnalyzerComponent {
    override val visitor: IrElementVisitorVoid = MyVisitor()

    private inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            TODO("not implemented")
        }
    }
}

class IfCondition : AnalyzerComponent {
    override val visitor: IrElementVisitorVoid = MyVisitor()

    private inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            TODO("not implemented")
        }
    }
}

class ThenBranch : CodeScope() {
    override val visitor: IrElementVisitorVoid
        get() = TODO("not implemented")
}

class ElseBranch : CodeScope() {
    override val visitor: IrElementVisitorVoid
        get() = TODO("not implemented")
}

class ForCycle : ScopeWithCallings() {
    override val visitor: IrElementVisitorVoid
        get() = TODO("not implemented")
}

class WhileCycle : ScopeWithCallings() {
    override val visitor: IrElementVisitorVoid = MyVisitor()

    private inner class MyVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {}

        override fun visitWhileLoop(loop: IrWhileLoop) {
            innerScopes.forEach { loop.body?.acceptChildrenVoid(it.visitor) }
        }
    }

}

class NewAnalyzer : AbstractScope() {
    fun functionBody(init: FunctionBody.() -> Unit): FunctionBody {
        val scope = FunctionBody()
        scope.init()
        innerScopes += scope
        return scope
    }

    override fun execute(irModule: IrModuleFragment, moduleDescriptor: ModuleDescriptor, bindingContext: BindingContext) {
        irModule.acceptChildrenVoid(visitor)
    }

    override val visitor: IrElementVisitorVoid = MyVisitor()

    private inner class MyVisitor : IrElementVisitorVoid {
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
