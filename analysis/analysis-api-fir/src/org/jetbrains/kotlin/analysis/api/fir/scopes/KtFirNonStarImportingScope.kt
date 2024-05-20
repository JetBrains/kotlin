/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractSimpleImportingScope
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KaFirNonStarImportingScope(
    firScope: FirAbstractSimpleImportingScope,
    builder: KaSymbolByFirBuilder,
) : KaFirBasedScope<FirAbstractSimpleImportingScope>(firScope, builder) {

    private val imports: List<NonStarImport> by cached {
        buildList {
            firScope.simpleImports.values.forEach { imports ->
                imports.forEach { import ->
                    val importedClassId = import.importedName?.let { importedName ->
                        val importedClassId =
                            import.resolvedParentClassId?.createNestedClassId(importedName) ?: ClassId(import.packageFqName, importedName)
                        importedClassId.takeIf { firScope.session.symbolProvider.getClassLikeSymbolByClassId(it) != null }
                    }
                    NonStarImport(
                        import.packageFqName,
                        importedClassId?.relativeClassName,
                        importedClassId,
                        import.importedName,
                        import.aliasName,
                    ).let(::add)
                }
            }
        }
    }

    override fun getPackageSymbols(nameFilter: KaScopeNameFilter): Sequence<KaPackageSymbol> = withValidityAssertion {
        emptySequence()
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = withValidityAssertion { emptySequence() }

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        imports.flatMapTo(hashSetOf()) { listOfNotNull(it.callableName, it.aliasName) }
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        imports.flatMapTo((hashSetOf())) { listOfNotNull(it.relativeClassName?.shortName(), it.aliasName) }
    }
}
