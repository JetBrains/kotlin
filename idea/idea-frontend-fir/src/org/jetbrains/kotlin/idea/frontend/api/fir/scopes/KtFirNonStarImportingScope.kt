/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultSimpleImportingScope
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwner
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwnerByValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtNonStarImportingScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.NonStarImport
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name

internal class KtFirNonStarImportingScope(
    firScope: FirAbstractSimpleImportingScope,
    builder: KtSymbolByFirBuilder,
    override val token: ValidityOwner
) : KtFirDelegatingScope(builder), KtNonStarImportingScope, ValidityOwnerByValidityToken {
    override val firScope: FirAbstractSimpleImportingScope = firScope

    @OptIn(ExperimentalStdlibApi::class)
    override val imports: List<NonStarImport> by cached {
        buildList {
            firScope.simpleImports.values.forEach { imports ->
                imports.forEach { import ->
                    NonStarImport(
                        import.packageFqName,
                        import.relativeClassName,
                        import.resolvedClassId,
                        import.importedName
                    ).let(::add)
                }
            }
        }
    }


    override fun getCallableNames(): Set<Name> = withValidityAssertion {
        imports.mapNotNullTo(hashSetOf()) { it.callableName }
    }

    override fun getClassLikeSymbolNames(): Set<Name> = withValidityAssertion {
        imports.mapNotNullTo((hashSetOf())) { it.relativeClassName?.shortName() }
    }


    override val isDefaultImportingScope: Boolean = withValidityAssertion { firScope is FirDefaultSimpleImportingScope }
}
