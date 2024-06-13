/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.scopes

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.name.Name

public interface KaScopeLike : KaLifetimeOwner {
    /**
     * Returns a **superset** of names which current scope may contain.
     * In other words `ALL_NAMES(scope)` is a subset of `scope.getAllNames()`
     */
    public fun getAllPossibleNames(): Set<Name> = withValidityAssertion {
        getPossibleCallableNames() + getPossibleClassifierNames()
    }

    /**
     * Returns a **superset** of callable names which current scope may contain.
     * In other words `ALL_CALLABLE_NAMES(scope)` is a subset of `scope.getCallableNames()`
     */
    public fun getPossibleCallableNames(): Set<Name>

    /**
     * Returns a **superset** of classifier names which current scope may contain.
     * In other words `ALL_CLASSIFIER_NAMES(scope)` is a subset of `scope.getClassifierNames()`
     */
    public fun getPossibleClassifierNames(): Set<Name>

    /**
     * return true if the scope may contain name, false otherwise.
     *
     * In other words `(mayContainName(name) == false) => (name !in scope)`; vice versa is not always true
     */
    public fun mayContainName(name: Name): Boolean = withValidityAssertion {
        name in getPossibleCallableNames() || name in getPossibleClassifierNames()
    }
}

public typealias KtScopeLike = KaScopeLike