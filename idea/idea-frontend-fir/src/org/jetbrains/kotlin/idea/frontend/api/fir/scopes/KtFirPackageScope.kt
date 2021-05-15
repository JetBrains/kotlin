/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.idea.fir.low.level.api.api.searchScope
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtPackageScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelClassByPackageIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionByPackageIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelPropertyByPackageIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelTypeAliasByPackageIndex
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KtFirPackageScope(
    firScope: FirPackageMemberScope,
    private val project: Project,
    private val builder: KtSymbolByFirBuilder,
    override val token: ValidityToken,
) : KtPackageScope {
    private val firScope by weakRef(firScope)
    override val fqName: FqName get() = firScope.fqName

    override fun getPossibleCallableNames() = withValidityAssertion {
        hashSetOf<Name>().apply {
            KotlinTopLevelPropertyByPackageIndex.getInstance()[fqName.asString(), project, firScope.session.searchScope]
                .mapNotNullTo(this) { it.nameAsName }
            KotlinTopLevelFunctionByPackageIndex.getInstance()[fqName.asString(), project, firScope.session.searchScope]
                .mapNotNullTo(this) { it.nameAsName }
        }
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        KotlinTopLevelClassByPackageIndex.getInstance()[fqName.asString(), project, firScope.session.searchScope]
            .mapNotNullTo(hashSetOf()) { it.nameAsName }

        KotlinTopLevelTypeAliasByPackageIndex.getInstance()[fqName.asString(), project, firScope.session.searchScope]
            .mapNotNullTo(hashSetOf()) { it.nameAsName }
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), builder)
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = emptySequence()
}
