/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.name.Name

/**
 * Excludes static callables and all classifiers.
 */
internal class FirNonStaticCallablesOnlyScope(
    private val delegate: FirContainingNamesAwareScope,
) : FirCallableFilteringScope(delegate) {
    override fun isTargetCallable(callable: FirCallableSymbol<*>): Boolean = !callable.fir.isStatic

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegate.processDeclaredConstructors(processor)
    }

    override fun getClassifierNames(): Set<Name> = emptySet()

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
    }
}
