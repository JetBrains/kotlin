/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives.model

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

sealed class DirectivesContainer {
    object Empty : SimpleDirectivesContainer()

    abstract operator fun get(name: String): Directive?
    abstract operator fun contains(directive: Directive): Boolean
}

abstract class SimpleDirectivesContainer : DirectivesContainer() {
    private val registeredDirectives: MutableMap<String, Directive> = mutableMapOf()

    override operator fun get(name: String): Directive? = registeredDirectives[name]

    protected fun directive(description: String): DirectiveDelegateProvider<SimpleDirective> {
        return DirectiveDelegateProvider { SimpleDirective(it, description) }
    }

    protected fun stringDirective(description: String): DirectiveDelegateProvider<StringDirective> {
        return DirectiveDelegateProvider { StringDirective(it, description) }
    }

    protected inline fun <reified T : Enum<T>> enumDirective(
        description: String,
        noinline additionalParser: ((String) -> T?)? = null
    ): DirectiveDelegateProvider<ValueDirective<T>> {
        val possibleValues = enumValues<T>()
        val parser: (String) -> T? = { value -> possibleValues.firstOrNull { it.name == value } ?: additionalParser?.invoke(value) }
        return DirectiveDelegateProvider { ValueDirective(it, description, parser) }
    }

    protected fun <T : Any> valueDirective(
        description: String,
        parser: (String) -> T?
    ): DirectiveDelegateProvider<ValueDirective<T>> {
        return DirectiveDelegateProvider { ValueDirective(it, description, parser) }
    }

    protected fun registerDirective(directive: Directive) {
        registeredDirectives[directive.name] = directive
    }

    override fun contains(directive: Directive): Boolean {
        return directive in registeredDirectives.values
    }

    override fun toString(): String {
        return buildString {
            appendLine("Directive container:")
            for (directive in registeredDirectives.values) {
                append("  ")
                appendLine(directive)
            }
        }
    }

    protected inner class DirectiveDelegateProvider<T : Directive>(val directiveConstructor: (String) -> T) {
        operator fun provideDelegate(
            thisRef: SimpleDirectivesContainer,
            property: KProperty<*>
        ): ReadOnlyProperty<SimpleDirectivesContainer, T> {
            val directive = directiveConstructor(property.name).also { thisRef.registerDirective(it) }
            return ReadOnlyProperty { _, _ -> directive }
        }
    }
}

class ComposedDirectivesContainer(private val containers: Collection<DirectivesContainer>) : DirectivesContainer() {
    constructor(vararg containers: DirectivesContainer) : this(containers.toList())

    override fun get(name: String): Directive? {
        for (container in containers) {
            container[name]?.let { return it }
        }
        return null
    }

    override fun contains(directive: Directive): Boolean {
        return containers.any { directive in it }
    }
}
