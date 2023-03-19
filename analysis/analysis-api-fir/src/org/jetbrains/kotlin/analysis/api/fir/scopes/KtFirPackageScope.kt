/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

internal class KtFirPackageScope(
    private val fqName: FqName,
    private val project: Project,
    private val builder: KtSymbolByFirBuilder,
    private val searchScope: GlobalSearchScope,
    private val declarationProvider: KotlinDeclarationProvider,
    private val targetPlatform: TargetPlatform,
) : KtScope {
    override val token: KtLifetimeToken get() = builder.token

    private val packageProvider = project.createPackageProvider(searchScope)

    private val firScope: FirPackageMemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        FirPackageMemberScope(fqName, builder.rootSession)
    }

    private val firExtensionService: FirExtensionService
        get() = firScope.session.extensionService

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        hashSetOf<Name>().apply {
            addAll(declarationProvider.getTopLevelCallableNamesInPackage(fqName))
            addAll(collectGeneratedTopLevelCallables())
        }
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        hashSetOf<Name>().apply {
            addAll(declarationProvider.getTopLevelKotlinClassLikeDeclarationNamesInPackage(fqName))

            JavaPsiFacade.getInstance(project)
                .findPackage(fqName.asString())
                ?.getClasses(searchScope)
                ?.mapNotNullTo(this) { it.name?.let(Name::identifier) }

            addAll(collectGeneratedTopLevelClassifiers())
        }
    }

    private fun collectGeneratedTopLevelCallables(): Set<Name> {
        val generators = firExtensionService.declarationGenerators

        val generatedTopLevelDeclarations = generators
            .asSequence()
            .flatMap {
                // FIXME this function should be called only once during plugin's lifetime, so this usage is not really correct (1)
                it.getTopLevelCallableIds()
            }
            .filter { it.packageName == fqName }
            .map { it.callableName }

        return generatedTopLevelDeclarations.toSet()
    }

    private fun collectGeneratedTopLevelClassifiers(): Set<Name> {
        val declarationGenerators = firExtensionService.declarationGenerators

        val generatedTopLevelClassifiers = declarationGenerators
            .asSequence()
            .flatMap {
                // FIXME this function should be called only once during plugin's lifetime, so this usage is not really correct (2)
                it.getTopLevelClassIds()
            }
            .filter { it.packageFqName == fqName }
            .map { it.shortClassName }

        return generatedTopLevelClassifiers.toSet()
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), builder)
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion {
        emptySequence()
    }

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = withValidityAssertion {
        sequence {
            packageProvider.getSubPackageFqNames(fqName, targetPlatform, nameFilter).forEach {
                yield(builder.createPackageSymbol(fqName.child(it)))
            }
        }
    }
}
