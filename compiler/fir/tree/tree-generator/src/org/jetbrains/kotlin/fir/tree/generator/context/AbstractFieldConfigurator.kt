/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.types.Variance

abstract class AbstractFieldConfigurator<T : AbstractFirTreeBuilder>(private val builder: T) {
    inner class ConfigureContext(val element: Element) {
        operator fun FieldSet.unaryPlus() {
            element.fields.addAll(this.map { it.copy() })
        }

        operator fun Field.unaryPlus() {
            val doesNotContains = element.fields.add(this.copy())
            require(doesNotContains) {
                "$element already contains field $this}"
            }
        }

        fun generateBooleanFields(vararg names: String) {
            names.forEach {
                +booleanField(if (it.startsWith("is") || it.startsWith("has")) it else "is${it.replaceFirstChar(Char::uppercaseChar)}")
            }
        }

        fun withArg(name: String, vararg upperBounds: TypeRef): TypeVariable = withArg(name, upperBounds.toList())

        fun withArg(name: String, upperBounds: List<TypeRef>, variance: Variance = Variance.INVARIANT): TypeVariable {
            return TypeVariable(name, upperBounds, variance).also(element.params::add)
        }

        fun parentArgs(parent: Element, vararg arguments: Pair<String, TypeRef>) {
            parentArgs(parent, listOf(*arguments))
        }

        private fun parentArgs(parent: Element, arguments: List<Pair<String, TypeRef>>) {
            parentArgs(parent, arguments.map { (name, arg) -> NamedTypeParameterRef(name) to arg })
        }

        @JvmName("parentArgs2")
        private fun parentArgs(parent: Element, arguments: List<Pair<NamedTypeParameterRef, TypeRef>>) {
            val parentIndex = element.elementParents.indexOfFirst { it.element == parent }
            require(parentIndex >= 0) {
                "$parent is not parent of $element"
            }
            val parentRef = element.elementParents[parentIndex]
            require(parentRef.args.isEmpty()) {
                "Parent $parent of element $element already has type arguments: $parentRef"
            }
            element.elementParents[parentIndex] = parentRef.copy(arguments.toMap())
        }

        fun needTransformOtherChildren() {
            element._needTransformOtherChildren = true
        }

        fun shouldBeAnInterface() {
            element.kind = ImplementationKind.Interface
        }

        fun shouldBeAbstractClass() {
            element.kind = ImplementationKind.AbstractClass
        }
    }

    fun Element.configure(block: ConfigureContext.() -> Unit) {
        builder.configurations[this] = { ConfigureContext(this).block() }
    }

    fun configure(init: T.() -> Unit) {
        builder.init()
        builder.applyConfigurations()
    }
}
