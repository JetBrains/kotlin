/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.impl

import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.directives.model.*

class RegisteredDirectivesParser(private val container: DirectivesContainer, private val assertions: Assertions) {
    companion object {
        private val DIRECTIVE_PATTERN = Regex("""^//\s*[!]?([A-Z0-9_]+)(:[ \t]*(.*))? *$""")
        private val SPACES_PATTERN = Regex("""[,]?[ \t]+""")
        private const val NAME_GROUP = 1
        private const val VALUES_GROUP = 3

        fun parseDirective(line: String): RawDirective? {
            val result = DIRECTIVE_PATTERN.matchEntire(line)?.groupValues ?: return null
            val name = result.getOrNull(NAME_GROUP) ?: return null
            val rawValue = result.getOrNull(VALUES_GROUP)
            val values = rawValue?.split(SPACES_PATTERN)?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            return RawDirective(name, values, rawValue)
        }
    }

    data class RawDirective(val name: String, val values: List<String>?, val rawValue: String?)
    data class ParsedDirective(val directive: Directive, val values: List<*>)

    private val simpleDirectives = mutableListOf<SimpleDirective>()
    private val stringValueDirectives = mutableMapOf<StringDirective, MutableList<String>>()
    private val valueDirectives = mutableMapOf<ValueDirective<*>, MutableList<Any>>()

    /**
     * returns true means that line contain directive
     */
    fun parse(line: String): Boolean {
        val rawDirective = parseDirective(line) ?: return false
        val parsedDirective = convertToRegisteredDirective(rawDirective) ?: return false
        addParsedDirective(parsedDirective)
        return true
    }

    fun addParsedDirective(parsedDirective: ParsedDirective) {
        val (directive, values) = parsedDirective
        @Suppress("UNCHECKED_CAST")
        when (directive) {
            is SimpleDirective -> simpleDirectives += directive
            is StringDirective -> {
                val list = stringValueDirectives.getOrPut(directive, ::mutableListOf)
                list += values as List<String>
            }
            is ValueDirective<*> -> {
                val list = valueDirectives.getOrPut(directive, ::mutableListOf)
                @Suppress("UNCHECKED_CAST")
                list.addAll(values as List<Any>)
            }
        }
    }

    fun convertToRegisteredDirective(rawDirective: RawDirective): ParsedDirective? {
        val (name, rawValues, rawValueString) = rawDirective
        val directive = container[name] ?: return null

        val values: List<*> = when (directive) {
            is SimpleDirective -> {
                if (rawValues != null) {
                    assertions.fail {
                        "Directive $directive should have no arguments, but ${rawValues.joinToString(", ")} are passed"
                    }
                }
                emptyList<Any?>()
            }

            is StringDirective -> {
                when (directive.multiLine) {
                    true -> listOfNotNull(rawValueString)
                    false -> rawValues ?: emptyList()
                }
            }

            is ValueDirective<*> -> {
                if (rawValues == null) {
                    assertions.fail {
                        "Directive $directive must have at least one value"
                    }
                }
                rawValues.map { directive.extractValue(it) ?: assertions.fail { "$it is not valid value for $directive" } }
            }
        }
        return ParsedDirective(directive, values)
    }

    private fun <T : Any> ValueDirective<T>.extractValue(name: String): T? {
        return parser.invoke(name)
    }

    fun build(): RegisteredDirectives {
        return RegisteredDirectivesImpl(simpleDirectives, stringValueDirectives, valueDirectives)
    }
}
