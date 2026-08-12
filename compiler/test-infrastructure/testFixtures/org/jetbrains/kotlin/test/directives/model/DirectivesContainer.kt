/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives.model

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@RequiresOptIn
annotation class SensitiveDirectiveAPI(val reason: String)

sealed class DirectivesContainer {
    object Empty : SimpleDirectivesContainer()

    abstract operator fun get(name: String): Directive?
    abstract operator fun contains(directive: Directive): Boolean
}

abstract class SimpleDirectivesContainerBase<D : Directive> : DirectivesContainer() {
    private val registeredDirectives: MutableMap<String, D> = mutableMapOf()

    val allDirectives: Collection<D>
        get() = registeredDirectives.values

    override operator fun get(name: String): D? = registeredDirectives[name]

    override fun contains(directive: Directive): Boolean {
        return directive in registeredDirectives.values
    }

    protected fun registerDirective(directive: D) {
        registeredDirectives[directive.name] = directive
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

    protected inner class DirectiveDelegateProvider<T : D>(val directiveConstructor: (String) -> T) {
        operator fun provideDelegate(
            thisRef: SimpleDirectivesContainerBase<D>,
            property: KProperty<*>
        ): ReadOnlyProperty<SimpleDirectivesContainerBase<D>, T> {
            val directive = directiveConstructor(property.name).also { thisRef.registerDirective(it) }
            return ReadOnlyProperty { _, _ -> directive }
        }
    }
}

abstract class SimpleDirectivesContainer : SimpleDirectivesContainerBase<Directive>() {
    protected fun directive(
        description: String,
        applicability: DirectiveApplicability = DirectiveApplicability.Global
    ): DirectiveDelegateProvider<SimpleDirective> {
        return DirectiveDelegateProvider { SimpleDirective(it, description, applicability) }
    }

    protected fun stringDirective(
        description: String,
        applicability: DirectiveApplicability = DirectiveApplicability.Global,
        multiLine: Boolean = false
    ): DirectiveDelegateProvider<StringDirective> {
        return DirectiveDelegateProvider { StringDirective(it, description, applicability, multiLine) }
    }

    protected inline fun <reified T : Enum<T>> enumDirective(
        description: String,
        applicability: DirectiveApplicability = DirectiveApplicability.Global,
        noinline additionalParser: ((String) -> T?)? = null
    ): DirectiveDelegateProvider<ValueDirective<T>> {
        val possibleValues = enumValues<T>()
        val parser: (String) -> T? = { value -> possibleValues.firstOrNull { it.name == value } ?: additionalParser?.invoke(value) }
        return DirectiveDelegateProvider { ValueDirective(it, description, applicability, parser, splitValuesOnSpaces = true) }
    }

    protected fun <T : Any> valueDirective(
        description: String,
        applicability: DirectiveApplicability = DirectiveApplicability.Global,
        parser: (String) -> T?,
    ): DirectiveDelegateProvider<ValueDirective<T>> {
        return DirectiveDelegateProvider { ValueDirective(it, description, applicability, parser, splitValuesOnSpaces = true) }
    }

    @SensitiveDirectiveAPI("Not splitting values of a directive on spaces should be well-thought out, to not introduce confusion with existing directives that do split on spaces")
    protected fun <T : Any> valueDirective(
        description: String,
        applicability: DirectiveApplicability = DirectiveApplicability.Global,
        splitValuesOnSpaces: Boolean,
        parser: (String) -> T?,
    ): DirectiveDelegateProvider<ValueDirective<T>> {
        return DirectiveDelegateProvider { ValueDirective(it, description, applicability, parser, splitValuesOnSpaces) }
    }
}

abstract class HomogenousValueDirectivesContainer<T : Any> : SimpleDirectivesContainerBase<ValueDirective<T>>() {
    protected fun valueDirective(
        description: String,
        applicability: DirectiveApplicability = DirectiveApplicability.Global,
        parser: (String) -> T?,
    ): DirectiveDelegateProvider<ValueDirective<T>> {
        return DirectiveDelegateProvider { ValueDirective(it, description, applicability, parser, splitValuesOnSpaces = true) }
    }

    @SensitiveDirectiveAPI("Not splitting values of a directive on spaces should be well-thought out, to not introduce confusion with existing directives that do split on spaces")
    protected fun valueDirective(
        description: String,
        applicability: DirectiveApplicability = DirectiveApplicability.Global,
        splitValuesOnSpaces: Boolean,
        parser: (String) -> T?,
    ): DirectiveDelegateProvider<ValueDirective<T>> {
        return DirectiveDelegateProvider { ValueDirective(it, description, applicability, parser, splitValuesOnSpaces) }
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
