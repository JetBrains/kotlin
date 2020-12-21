/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.builders

import org.jetbrains.kotlin.test.directives.model.*

class RegisteredDirectivesBuilder {
    private val simpleDirectives: MutableList<SimpleDirective> = mutableListOf()
    private val stringDirectives: MutableMap<StringDirective, List<String>> = mutableMapOf()
    private val valueDirectives: MutableMap<ValueDirective<*>, List<Any>> = mutableMapOf()

    operator fun SimpleDirective.unaryPlus() {
        simpleDirectives += this
    }

    infix fun StringDirective.with(value: String) {
        with(listOf(value))
    }

    infix fun StringDirective.with(values: List<String>) {
        stringDirectives.putWithExistsCheck(this, values)
    }

    infix fun <T : Any> ValueDirective<T>.with(value: T) {
        with(listOf(value))
    }

    infix fun <T : Any> ValueDirective<T>.with(values: List<T>) {
        valueDirectives.putWithExistsCheck(this, values)
    }

    private fun <K : Directive, V> MutableMap<K, V>.putWithExistsCheck(key: K, value: V) {
        val alreadyRegistered = put(key, value)
        if (alreadyRegistered != null) {
            error("Default values for $key directive already registered")
        }
    }

    fun build(): RegisteredDirectives {
        return RegisteredDirectivesImpl(simpleDirectives, stringDirectives, valueDirectives)
    }
}
