/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

class FirStaticScope(private val delegateScope: FirContainingNamesAwareScope) : FirContainingNamesAwareScope() {
    // We want to *avoid* delegation to certain scope functions, so we delegate explicitly instead of using
    // `FirDelegatingContainingNamesAwareScope`.

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        delegateScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        delegateScope.processFunctionsByName(name) {
            if (it.fir.isStatic) {
                processor(it)
            }
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        delegateScope.processPropertiesByName(name) {
            if (it.fir.isStatic) {
                processor(it)
            }
        }
    }


    override fun getCallableNames(): Set<Name> {
        return delegateScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return delegateScope.getClassifierNames()
    }
}
