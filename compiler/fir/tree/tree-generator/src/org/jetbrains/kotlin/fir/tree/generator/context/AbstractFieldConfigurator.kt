/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.tree.generator.model.*

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

        fun withArg(name: String, vararg upperBounds: String) {
            withArg(name, upperBounds.map { Type(null, it) })
        }

        fun withArg(name: String, upperBound: Importable, vararg upperBounds: Importable) {
            val allUpperBounds = mutableListOf(upperBound).apply { this += upperBounds }
            withArg(name, allUpperBounds)
        }

        private fun withArg(name: String, upperBounds: List<Importable>) {
            element.typeArguments += when (upperBounds.size) {
                in 0..1 -> SimpleTypeArgument(name, upperBounds.firstOrNull())
                else -> TypeArgumentWithMultipleUpperBounds(name, upperBounds.toList())
            }
        }

        fun parentArg(parent: Element, argument: String, type: String) {
            parentArg(parent, Type(null, argument), Type(null, type))
        }

        fun parentArg(parent: Element, argument: String, type: Importable) {
            parentArg(parent, Type(null, argument), type)
        }

        fun parentArg(parent: Element, argument: Importable, type: Importable) {
            require(parent in element.parents) {
                "$parent is not parent of $element"
            }
            val argMap = element.parentsArguments.getOrPut(parent) { mutableMapOf() }
            require(argument !in argMap) {
                "Argument $argument already defined for parent $parent of $element"
            }
            argMap[argument] = type
        }

        fun Type.withArgs(vararg args: Importable): Pair<Type, List<Importable>> {
            return this to args.toList()
        }

        fun Type.withArgs(vararg args: String): Pair<Type, List<Importable>> {
            return this to args.map { Type(null, it) }
        }

        fun needTransformOtherChildren() {
            element._needTransformOtherChildren = true
        }

        fun shouldBeAnInterface() {
            element.kind = Implementation.Kind.Interface
        }

        fun shouldBeAbstractClass() {
            element.kind = Implementation.Kind.AbstractClass
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
