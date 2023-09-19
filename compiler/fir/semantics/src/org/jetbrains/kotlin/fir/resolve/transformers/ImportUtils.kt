/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedParentInImport
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

fun resolveToPackageOrClass(symbolProvider: FirSymbolProvider, fqName: FqName): PackageResolutionResult {
    var currentPackage = fqName

    val pathSegments = fqName.pathSegments()
    var prefixSize = pathSegments.size
    while (!currentPackage.isRoot && prefixSize > 0) {
        if (symbolProvider.getPackage(currentPackage) != null) {
            break
        }
        currentPackage = currentPackage.parent()
        prefixSize--
    }

    if (currentPackage == fqName) return PackageResolutionResult.PackageOrClass(currentPackage, null, null)
    val relativeClassFqName = FqName.fromSegments((prefixSize until pathSegments.size).map { pathSegments[it].asString() })

    val classId = ClassId(currentPackage, relativeClassFqName, isLocal = false)
    val symbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return PackageResolutionResult.Error(
        ConeUnresolvedParentInImport(classId)
    )

    return PackageResolutionResult.PackageOrClass(currentPackage, relativeClassFqName, symbol)
}

sealed class PackageResolutionResult {
    data class PackageOrClass(
        val packageFqName: FqName, val relativeClassFqName: FqName?, val classSymbol: FirClassLikeSymbol<*>?
    ) : PackageResolutionResult()

    class Error(val diagnostic: ConeDiagnostic) : PackageResolutionResult()
}
