/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.scopes

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KaFe10PackageSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.resolve.scopes.MemberScope

internal class KaFe10PackageScope(
    scope: MemberScope,
    private val owner: KaPackageSymbol,
    analysisContext: Fe10AnalysisContext
) : KaFe10ScopeMember(scope, constructors = emptyList(), analysisContext) {
    override fun getPackageSymbols(nameFilter: KaScopeNameFilter): Sequence<KaPackageSymbol> = withValidityAssertion {
        val packageFragmentProvider = analysisContext.resolveSession.packageFragmentProvider
        return packageFragmentProvider.getSubPackagesOf(owner.fqName, nameFilter)
            .asSequence()
            .map { KaFe10PackageSymbol(it, analysisContext) }
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = withValidityAssertion {
        return emptySequence()
    }
}