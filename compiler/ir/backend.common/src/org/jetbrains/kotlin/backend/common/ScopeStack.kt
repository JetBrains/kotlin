/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

internal class ScopeStack<E> {
    private class Scope<E>(val isGlobal: Boolean) {
        val values = mutableSetOf<E>()
    }

    // The outer list is never empty
    private val scopes = mutableListOf<MutableList<Scope<E>>>(mutableListOf())

    private fun <R> withNestedScope(isGlobalScope: Boolean, populateScope: MutableSet<E>.() -> Unit = {}, block: () -> R): R =
        scopes.last().temporarilyPushing(Scope(isGlobalScope)) {
            it.values.populateScope()
            block()
        }

    /**
     * Creates a new scope and runs [block] with it.
     *
     * @param isGlobalScope Whether values of this new scope are always visible in nested scopes,
     *   even for those nested scopes where [outerScopesAreInvisible] is `true`.
     * @param outerScopesAreInvisible Whether values of outer scopes are invisible in this new scope, except when an outer scope is global.
     */
    fun <R> withNewScope(
        isGlobalScope: Boolean = false,
        outerScopesAreInvisible: Boolean = false,
        populateScope: MutableSet<E>.() -> Unit = {},
        block: () -> R,
    ): R =
        if (outerScopesAreInvisible) {
            scopes.temporarilyPushing(scopes.last().filterTo(mutableListOf(), Scope<E>::isGlobal)) {
                withNestedScope(isGlobalScope = false, populateScope, block)
            }
        } else {
            withNestedScope(isGlobalScope, populateScope, block)
        }

    fun addToCurrentScope(element: E) {
        scopes.last().lastOrNull()?.values?.add(element)
    }

    fun isVisibleInCurrentScope(element: E): Boolean = scopes.last().any { it.values.contains(element) }
}
