/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaTypeScope

public abstract class KaScopeSubstitution : KaSessionComponent() {
    public abstract fun getDeclarationScope(scope: KaTypeScope): KaScope
}

public typealias KtScopeSubstitution = KaScopeSubstitution

public interface KaScopeSubstitutionMixIn : KaSessionMixIn {
    public fun KaTypeScope.getDeclarationScope(): KaScope =
        withValidityAssertion { analysisSession.scopeSubstitution.getDeclarationScope(this) }
}

public typealias KtScopeSubstitutionMixIn = KaScopeSubstitutionMixIn