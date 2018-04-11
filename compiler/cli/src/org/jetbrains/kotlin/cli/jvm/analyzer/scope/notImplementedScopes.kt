/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.analyzer.scope

import org.jetbrains.kotlin.ir.IrElement

open class ClassPredicate : ScopePredicate() {
    override val visitor: Visitor
        get() = TODO("not implemented")

    override fun checkIrNode(element: IrElement): Boolean {
        TODO("not implemented")
    }

    fun propertyDefinition(init: PropertyPredicate.() -> Unit): PropertyPredicate {
        val scope = PropertyPredicate()
        scope.init()
        innerPredicates += scope
        return scope
    }
}

class ObjectPredicate : ClassPredicate()

class InterfacePredicate : ClassPredicate()

class PropertyPredicate : AbstractPredicate() {
    override val visitor: Visitor = MyVisitor()

    override fun checkIrNode(element: IrElement): Boolean {
        TODO("not implemented")
    }

    inner class MyVisitor : Visitor {
        override fun visitElement(element: IrElement, data: VisitorData) {
            TODO("not implemented")
        }
    }
}