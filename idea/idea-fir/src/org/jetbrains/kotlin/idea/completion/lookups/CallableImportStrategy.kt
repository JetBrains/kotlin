/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups

import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.addImportToFile
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtPossibleMemberSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.KtFile

internal sealed class CallableImportStrategy {
    object DoNothing : CallableImportStrategy()
    data class AddImport(val nameToImport: CallableId) : CallableImportStrategy()
    data class InsertFqNameAndShorten(val callableId: CallableId) : CallableImportStrategy()
}

internal fun detectImportStrategy(symbol: KtCallableSymbol): CallableImportStrategy {
    if (symbol !is KtPossibleMemberSymbol || symbol.dispatchType != null) return CallableImportStrategy.DoNothing

    val propertyId = symbol.callableIdIfNonLocal ?: return CallableImportStrategy.DoNothing

    return if (symbol.isExtension) {
        CallableImportStrategy.AddImport(propertyId)
    } else {
        CallableImportStrategy.InsertFqNameAndShorten(propertyId)
    }
}

internal fun addCallableImportIfRequired(targetFile: KtFile, nameToImport: CallableId) {
    if (!alreadyHasImport(targetFile, nameToImport)) {
        addImportToFile(targetFile.project, targetFile, nameToImport)
    }
}

private fun alreadyHasImport(file: KtFile, nameToImport: CallableId): Boolean {
    if (file.importDirectives.any { it.importPath?.fqName == nameToImport.asSingleFqName() }) return true

    withAllowedResolve {
        analyse(file) {
            val scopes = file.getScopeContextForFile().scopes
            if (!scopes.mayContainName(nameToImport.callableName)) return false

            return scopes
                .getCallableSymbols { it == nameToImport.callableName }
                .any {
                    it is KtKotlinPropertySymbol && it.callableIdIfNonLocal == nameToImport ||
                            it is KtFunctionSymbol && it.callableIdIfNonLocal == nameToImport
                }
        }
    }
}