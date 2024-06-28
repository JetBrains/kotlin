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

/**
 * Compared to [resolveToPackageOrClass], does not perform the actual resolve.
 *
 * Instead of it, it just looks for the longest existing package name prefix in the [fqName],
 * and assumes that the rest of the name (if present) is a relative class name.
 *
 * Given that [FqName.ROOT] package is always present in any [FirSymbolProvider],
 * this function **can never fail**.
 */
fun findLongestExistingPackage(symbolProvider: FirSymbolProvider, fqName: FqName): PackageAndClass {
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

    if (currentPackage == fqName) return PackageAndClass(currentPackage, relativeClassFqName = null)
    val relativeClassFqName = FqName.fromSegments((prefixSize until pathSegments.size).map { pathSegments[it].asString() })

    return PackageAndClass(currentPackage, relativeClassFqName)
}

data class PackageAndClass(val packageFqName: FqName, val relativeClassFqName: FqName?)

fun resolveToPackageOrClass(symbolProvider: FirSymbolProvider, fqName: FqName): PackageResolutionResult {
    val (currentPackage, relativeClassFqName) = findLongestExistingPackage(symbolProvider, fqName)
    if (relativeClassFqName == null) return PackageResolutionResult.PackageOrClass(currentPackage, null, null)

    val classId = ClassId(currentPackage, relativeClassFqName, isLocal = false)

    return resolveToPackageOrClass(symbolProvider, classId)
}

fun resolveToPackageOrClass(symbolProvider: FirSymbolProvider, classId: ClassId): PackageResolutionResult {
    val symbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return PackageResolutionResult.Error(
        ConeUnresolvedParentInImport(classId)
    )

    return PackageResolutionResult.PackageOrClass(classId.packageFqName, classId.relativeClassName, symbol)
}

sealed class PackageResolutionResult {
    data class PackageOrClass(
        val packageFqName: FqName, val relativeClassFqName: FqName?, val classSymbol: FirClassLikeSymbol<*>?
    ) : PackageResolutionResult()

    class Error(val diagnostic: ConeDiagnostic) : PackageResolutionResult()
}
