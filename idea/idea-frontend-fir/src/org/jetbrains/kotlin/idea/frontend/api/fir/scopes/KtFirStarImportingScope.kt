/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.scopes.getContainingCallableNamesIfPresent
import org.jetbrains.kotlin.fir.scopes.getContainingClassifierNamesIfPresent
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.scopes.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelClassByPackageIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionByPackageIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelPropertyByPackageIndex
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject

internal class KtFirStarImportingScope(
    firScope: FirAbstractStarImportingScope,
    private val builder: KtSymbolByFirBuilder,
    project: Project,
    override val token: ValidityToken,
) : KtStarImportingScope, ValidityTokenOwner {
    private val firScope: FirAbstractStarImportingScope by weakRef(firScope)
    override val isDefaultImportingScope: Boolean = withValidityAssertion { firScope is FirDefaultStarImportingScope }
    private val packageHelper = PackageIndexHelper(project)

    override val imports: List<StarImport> by cached {
        firScope.starImports.map { import ->
            StarImport(
                import.packageFqName,
                import.relativeClassName,
                import.resolvedClassId
            )
        }
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), builder)
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = emptySequence()

    // todo cache?
    @OptIn(ExperimentalStdlibApi::class)
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        imports.flatMapTo(hashSetOf()) { import: Import ->
            if (import.relativeClassName == null) { // top level callable
                packageHelper.getPackageTopLevelCallables(import.packageFqName)
            } else { //member
                val classId = import.resolvedClassId ?: error("Class id should not be null as relativeClassName is not null")
                firScope.getStaticsScope(classId)?.getContainingCallableNamesIfPresent().orEmpty()
            }
        }
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        imports.flatMapTo(hashSetOf()) { import ->
            if (import.relativeClassName == null) {
                packageHelper.getPackageTopLevelClassifiers(import.packageFqName)
            } else {
                val classId = import.resolvedClassId ?: error("Class id should not be null as relativeClassName is not null")
                firScope.getStaticsScope(classId)?.getContainingClassifierNamesIfPresent().orEmpty()
            }
        }
    }

}

private class PackageIndexHelper(private val project: Project) {
    //todo use more concrete scope
    private val searchScope = GlobalSearchScope.allScope(project)

    private val functionByPackageIndex = KotlinTopLevelFunctionByPackageIndex.getInstance()
    private val propertyByPackageIndex = KotlinTopLevelPropertyByPackageIndex.getInstance()

    private val classByPackageIndex = KotlinTopLevelClassByPackageIndex.getInstance()

    fun getPackageTopLevelCallables(packageFqName: FqName): Set<Name> {
        return getTopLevelCallables(packageFqName).mapTo(hashSetOf()) { it.nameAsSafeName }
    }

    fun getPackageTopLevelClassifiers(packageFqName: FqName): Set<Name> {
        return getTopLevelClassifiers(packageFqName).mapTo(hashSetOf()) { it.nameAsSafeName }
    }

    private fun getTopLevelClassifiers(packageFqName: FqName): MutableCollection<KtClassOrObject> =
        classByPackageIndex.get(packageFqName.asString(), project, searchScope)

    private fun getTopLevelCallables(packageFqName: FqName): Sequence<KtCallableDeclaration> = sequence<KtCallableDeclaration> {
        yieldAll(functionByPackageIndex.get(packageFqName.asString(), project, searchScope))
        yieldAll(propertyByPackageIndex.get(packageFqName.asString(), project, searchScope))
    }
}