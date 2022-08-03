/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.scopes

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

internal open class KtFe10MemberScope(
    private val scope: MemberScope,
    analysisContext: Fe10AnalysisContext
) : KtFe10ResolutionScope(analysisContext) {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        return scope.getFunctionNames() + scope.getVariableNames()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return scope.getClassifierNames() ?: emptySet()
    }

    override fun getRawCallableSymbols(nameFilter: KtScopeNameFilter): Collection<DeclarationDescriptor> {
        return scope.getContributedDescriptors(kindFilter = DescriptorKindFilter.ALL, nameFilter)
    }

    override fun getRawClassifierSymbols(nameFilter: KtScopeNameFilter): Collection<DeclarationDescriptor> {
        return scope.getContributedDescriptors(kindFilter = DescriptorKindFilter.CLASSIFIERS, nameFilter)
    }

    override fun getRawConstructorSymbols(): Collection<DeclarationDescriptor> {
        return scope.getContributedDescriptors(kindFilter = DescriptorKindFilter.FUNCTIONS)
    }
}