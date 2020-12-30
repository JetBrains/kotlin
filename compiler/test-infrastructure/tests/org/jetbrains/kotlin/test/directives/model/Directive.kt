/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives.model

import org.jetbrains.kotlin.test.util.joinToArrayString

// --------------------------- Directive declaration ---------------------------

sealed class Directive(val name: String, val description: String) {
    override fun toString(): String {
        return name
    }
}

class SimpleDirective(
    name: String,
    description: String
) : Directive(name, description)

class StringDirective(
    name: String,
    description: String
) : Directive(name, description)

class ValueDirective<T : Any>(
    name: String,
    description: String,
    val parser: (String) -> T?
) : Directive(name, description)

// --------------------------- Registered directive ---------------------------

abstract class RegisteredDirectives {
    companion object {
        val Empty = RegisteredDirectivesImpl(emptyList(), emptyMap(), emptyMap())
    }

    abstract operator fun contains(directive: Directive): Boolean
    abstract operator fun get(directive: StringDirective): List<String>
    abstract operator fun <T : Any> get(directive: ValueDirective<T>): List<T>

    abstract fun isEmpty(): Boolean
}

class RegisteredDirectivesImpl(
    private val simpleDirectives: List<SimpleDirective>,
    private val stringDirectives: Map<StringDirective, List<String>>,
    private val valueDirectives: Map<ValueDirective<*>, List<Any>>
) : RegisteredDirectives() {
    override operator fun contains(directive: Directive): Boolean {
        return when (directive) {
            is SimpleDirective -> directive in simpleDirectives
            is StringDirective -> directive in stringDirectives
            is ValueDirective<*> -> directive in valueDirectives
        }
    }

    override operator fun get(directive: StringDirective): List<String> {
        return stringDirectives[directive] ?: emptyList()
    }

    override fun <T : Any> get(directive: ValueDirective<T>): List<T> {
        @Suppress("UNCHECKED_CAST")
        return valueDirectives[directive] as List<T>? ?: emptyList()
    }

    override fun isEmpty(): Boolean {
        return simpleDirectives.isEmpty() && stringDirectives.isEmpty() && valueDirectives.isEmpty()
    }

    override fun toString(): String {
        return buildString {
            simpleDirectives.forEach { appendLine("  $it") }
            stringDirectives.forEach { (d, v) -> appendLine("  $d: ${v.joinToArrayString()}") }
            valueDirectives.forEach { (d, v) -> appendLine("  $d: ${v.joinToArrayString()}")}
        }
    }
}

class ComposedRegisteredDirectives(
    private val containers: List<RegisteredDirectives>
) : RegisteredDirectives() {
    companion object {
        operator fun invoke(vararg containers: RegisteredDirectives): RegisteredDirectives {
            val notEmptyContainers = containers.filterNot { it.isEmpty() }
            return when (notEmptyContainers.size) {
                0 -> Empty
                1 -> notEmptyContainers.single()
                else -> ComposedRegisteredDirectives(notEmptyContainers)
            }
        }
    }

    override fun contains(directive: Directive): Boolean {
        return containers.any { directive in it }
    }

    override fun get(directive: StringDirective): List<String> {
        return containers.flatMap { it[directive] }
    }

    override fun <T : Any> get(directive: ValueDirective<T>): List<T> {
        return containers.flatMap { it[directive] }
    }

    override fun isEmpty(): Boolean {
        return containers.all { it.isEmpty() }
    }
}

// --------------------------- Utils ---------------------------

fun RegisteredDirectives.singleValue(directive: StringDirective): String {
    return singleOrZeroValue(directive) ?: error("No values passed to $directive")
}

fun RegisteredDirectives.singleOrZeroValue(directive: StringDirective): String? {
    val values = this[directive]
    return when (values.size) {
        0 -> null
        1 -> values.single()
        else -> error("Too many values passed to $directive")
    }
}

fun <T : Any> RegisteredDirectives.singleValue(directive: ValueDirective<T>): T {
    return singleOrZeroValue(directive) ?: error("No values passed to $directive")
}

fun <T : Any> RegisteredDirectives.singleOrZeroValue(directive: ValueDirective<T>): T? {
    val values = this[directive]
    return when (values.size) {
        0 -> null
        1 -> values.single()
        else -> error("Too many values passed to $directive")
    }
}
