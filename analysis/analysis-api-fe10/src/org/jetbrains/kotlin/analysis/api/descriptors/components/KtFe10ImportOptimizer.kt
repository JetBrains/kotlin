/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtImportOptimizer
import org.jetbrains.kotlin.analysis.api.components.KtImportOptimizerResult
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective

internal class KtFe10ImportOptimizer(private val analysisSession: KtFe10AnalysisSession) : KtImportOptimizer() {
    override val token: ValidityToken
        get() = analysisSession.token

    override fun analyseImports(file: KtFile): KtImportOptimizerResult = withValidityAssertion {
        val (allUnderImports, otherImports) = file.importDirectives.partition { it.isAllUnder }

        val unusedImports = LinkedHashSet<KtImportDirective>()
        val importedPackages = HashSet<FqName>()

        for (import in allUnderImports) {
            val fqName = import.importedFqName ?: continue
            if (!importedPackages.add(fqName)) {
                unusedImports += import
            }
        }

        for (import in otherImports) {
            val fqName = import.importedFqName ?: continue
            if (import.alias == null && fqName.parent() in importedPackages) {
                unusedImports += import
            }
        }

        return KtImportOptimizerResult(unusedImports)
    }
}