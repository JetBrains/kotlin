/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

internal class ScopeStack<E> {
    private class Scope<E>(
        val isGlobal: Boolean,
        val isOuterScopeInvisible: Boolean,
    ) {
        val values = mutableSetOf<E>()
    }

    // The outer list is never empty
    private val scopes = mutableListOf<Scope<E>>()

    fun pushScope(
        isGlobalScope: Boolean = false,
        outerScopesAreInvisible: Boolean = false,
        populateScope: MutableSet<E>.() -> Unit = {}
    ) {
        scopes.add(Scope(isGlobalScope, outerScopesAreInvisible))
        scopes.last().values.populateScope()
    }

    fun popScope() {
        scopes.pop()
    }

    fun addToCurrentScope(element: E) {
        scopes.last().values.add(element)
    }

    fun isVisibleInCurrentScope(element: E): Boolean {
        var isHidden = false
        for (scope in scopes.asReversed()) {
            if ((!isHidden || scope.isGlobal) && element in scope.values) return true
            if (scope.isOuterScopeInvisible) {
                isHidden = true
            }
        }
        return false
    }
}
