/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.addImportToFile
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtPossibleMemberSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

internal sealed class ImportStrategy {
    object DoNothing : ImportStrategy()
    data class AddImport(val nameToImport: FqName) : ImportStrategy()
    data class InsertFqNameAndShorten(val fqName: FqName) : ImportStrategy()
}

internal fun KtAnalysisSession.detectImportStrategy(symbol: KtSymbol): ImportStrategy = when (symbol) {
    is KtCallableSymbol -> detectImportStrategyForCallableSymbol(symbol)
    is KtClassLikeSymbol -> detectImportStrategyForClassLikeSymbol(symbol)
    else -> ImportStrategy.DoNothing
}

internal fun KtAnalysisSession.detectImportStrategyForCallableSymbol(symbol: KtCallableSymbol): ImportStrategy {
    if (symbol !is KtPossibleMemberSymbol || symbol.dispatchType != null) return ImportStrategy.DoNothing

    val propertyId = symbol.callableIdIfNonLocal?.asSingleFqName() ?: return ImportStrategy.DoNothing

    return if (symbol.isExtension) {
        ImportStrategy.AddImport(propertyId)
    } else {
        ImportStrategy.InsertFqNameAndShorten(propertyId)
    }
}

internal fun KtAnalysisSession.detectImportStrategyForClassLikeSymbol(symbol: KtClassLikeSymbol): ImportStrategy {
    val classId = symbol.classIdIfNonLocal ?: return ImportStrategy.DoNothing
    return ImportStrategy.InsertFqNameAndShorten(classId.asSingleFqName())
}

internal fun addCallableImportIfRequired(targetFile: KtFile, nameToImport: FqName) {
    if (!alreadyHasImport(targetFile, nameToImport)) {
        addImportToFile(targetFile.project, targetFile, nameToImport)
    }
}

private fun alreadyHasImport(file: KtFile, nameToImport: FqName): Boolean {
    if (file.importDirectives.any { it.importPath?.fqName == nameToImport }) return true

    withAllowedResolve {
        analyse(file) {
            val scopes = file.getScopeContextForFile().scopes
            if (!scopes.mayContainName(nameToImport.shortName())) return false

            val anyCallableSymbolMatches = scopes
                .getCallableSymbols { it == nameToImport.shortName() }
                .any { callable ->
                    val callableFqName = callable.callableIdIfNonLocal?.asSingleFqName()
                    callable is KtKotlinPropertySymbol && callableFqName == nameToImport ||
                            callable is KtFunctionSymbol && callableFqName == nameToImport
                }
            if (anyCallableSymbolMatches) return true

            return scopes.getClassifierSymbols { it == nameToImport.shortName() }.any { classifier ->
                val classId = (classifier as? KtClassLikeSymbol)?.classIdIfNonLocal
                classId?.asSingleFqName() == nameToImport
            }
        }
    }
}