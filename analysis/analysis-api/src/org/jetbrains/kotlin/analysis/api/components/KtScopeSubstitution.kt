/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtTypeScope

public abstract class KtScopeSubstitution : KtAnalysisSessionComponent() {
    public abstract fun getDeclarationScope(scope: KtTypeScope): KtScope
}

public interface KtScopeSubstitutionMixIn : KtAnalysisSessionMixIn {
    public fun KtTypeScope.getDeclarationScope(): KtScope =
        withValidityAssertion { analysisSession.scopeSubstitution.getDeclarationScope(this) }
}
