/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.processAllCallables
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

/**
 * A base implementation for [FirNonStaticMembersScope] and [FirJavaDeclaredMembersOnlyScope], which both filter callables based on some
 * condition.
 */
internal abstract class FirCallableFilteringScope(private val baseScope: FirContainingNamesAwareScope) : FirContainingNamesAwareScope() {
    protected abstract fun isTargetCallable(callable: FirCallableSymbol<*>): Boolean

    private val cachedCallableNames by lazy(LazyThreadSafetyMode.NONE) {
        buildSet {
            baseScope.processAllCallables { callable ->
                if (isTargetCallable(callable)) {
                    add(callable.name)
                }
            }
        }
    }

    override fun getCallableNames(): Set<Name> = cachedCallableNames

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        if (!cachedCallableNames.contains(name)) return

        baseScope.processFunctionsByName(name) { function ->
            if (isTargetCallable(function)) {
                processor(function)
            }
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        if (!cachedCallableNames.contains(name)) return

        baseScope.processPropertiesByName(name) { property ->
            if (isTargetCallable(property)) {
                processor(property)
            }
        }
    }
}
