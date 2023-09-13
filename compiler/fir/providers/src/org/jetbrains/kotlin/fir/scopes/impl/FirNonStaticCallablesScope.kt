/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirDelegatingContainingNamesAwareScope
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

/**
 * A counterpart to [FirStaticScope] that contains only the *non-static* callables and all classifiers of the [delegate] scope.
 */
class FirNonStaticCallablesScope(private val delegate: FirContainingNamesAwareScope) : FirDelegatingContainingNamesAwareScope(delegate) {
    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        delegate.processFunctionsByName(name) {
            if (!it.fir.isStatic) {
                processor(it)
            }
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        delegate.processPropertiesByName(name) {
            if (!it.fir.isStatic) {
                processor(it)
            }
        }
    }
}
