/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

/**
 * Marker interface for session components
 */
interface FirSessionComponent

/**
 * Marker interface for session components which could have different implementations depending
 * on the target (e.g. different implementations for JVM and Native). Such components should be
 * able to be composed with each other in case of metadata compilation for several targets.
 */
interface FirComposableSessionComponent<T : FirComposableSessionComponent<T>> : FirSessionComponent {
    @SessionConfiguration
    fun compose(other: T): T {
        val components = buildList {
            addAll(components)
            addAll(other.components)
        }.distinct()
        components.singleOrNull()?.let { return it }
        @Suppress("UNCHECKED_CAST")
        return createComposed(components) as T
    }

    @Suppress("UNCHECKED_CAST")
    val components: List<T>
        get() = listOf(this as T)

    @SessionConfiguration
    fun createComposed(components: List<T>): Composed<T>

    interface Composed<T : FirComposableSessionComponent<T>> : FirComposableSessionComponent<T> {
        override val components: List<T>
    }
}
