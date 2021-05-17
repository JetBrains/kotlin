/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.idea.fir.low.level.api.api.searchScope
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.components.KtFirScopeProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.lazyThreadUnsafeWeakRef
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtPackageScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.idea.search.getKotlinFqName
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm

internal class KtFirPackageScope(
    override val fqName: FqName,
    private val project: Project,
    private val builder: KtSymbolByFirBuilder,
    _scopeProvider: KtFirScopeProvider,
    override val token: ValidityToken,
    private val searchScope: GlobalSearchScope,
    private val targetPlatform: TargetPlatform,
) : KtPackageScope {
    private val scopeProvider by weakRef(_scopeProvider)

    private val firScope: FirPackageMemberScope by lazyThreadUnsafeWeakRef {
        val scope = FirPackageMemberScope(fqName, builder.rootSession)
        scopeProvider.registerScope(scope)
        scope
    }

    override fun getPossibleCallableNames() = withValidityAssertion {
        hashSetOf<Name>().apply {
            KotlinTopLevelPropertyByPackageIndex.getInstance()[fqName.asString(), project, searchScope]
                .mapNotNullTo(this) { it.nameAsName }
            KotlinTopLevelFunctionByPackageIndex.getInstance()[fqName.asString(), project, searchScope]
                .mapNotNullTo(this) { it.nameAsName }
        }
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        hashSetOf<Name>().apply {
            KotlinTopLevelClassByPackageIndex.getInstance()[fqName.asString(), project, searchScope]
                .mapNotNullTo(this) { it.nameAsName }

            KotlinTopLevelTypeAliasByPackageIndex.getInstance()[fqName.asString(), project, searchScope]
                .mapNotNullTo(this) { it.nameAsName }
        }
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), builder)
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
    }

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = withValidityAssertion {
        sequence {
            PackageIndexUtil
                .getSubPackageFqNames(fqName, searchScope, project, nameFilter)
                .forEach {
                    yield(builder.createPackageSymbol(it))
                }

            if (targetPlatform.isJvm()) {
                JavaPsiFacade.getInstance(project).findPackage(fqName.asString())?.getSubPackages(searchScope)?.forEach { psiPackage ->
                    val fqName = psiPackage.getKotlinFqName() ?: return@forEach
                    if (nameFilter(fqName.shortName())) {
                        yield(builder.createPackageSymbol(fqName))
                    }
                }
            }
        }
    }
}
