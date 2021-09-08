/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.rendering

// holds data about the parameters of the diagnostic we're about to render
sealed class RenderingContext {
    abstract operator fun <T> get(key: Key<T>): T

    abstract class Key<out T>(val name: String) {
        abstract fun compute(objectsToRender: Collection<Any?>): T
    }

    class Impl(private val objectsToRender: Collection<Any?>) : RenderingContext() {
        private val data = linkedMapOf<Key<*>, Any?>()

        @Suppress("UNCHECKED_CAST")
        override fun <T> get(key: Key<T>): T {
            return data[key] as? T ?: key.compute(objectsToRender).also { data[key] = it }
        }
    }

    object Empty : RenderingContext() {
        override fun <T> get(key: Key<T>): T {
            return key.compute(emptyList())
        }
    }

    companion object {
        @JvmStatic
        fun of(vararg objectsToRender: Any?): RenderingContext {
            return Impl(objectsToRender.toList())
        }
    }
}
