/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.classIdOrError
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

private const val KOTLIN_PACKAGE_PREFIX = "kotlin."

internal fun ClassId.isKotlinPackage(): Boolean = startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)

internal fun FqName.isKotlinPackage(): Boolean = startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)

internal fun String.isKotlinPackage(): Boolean = startsWith(KOTLIN_PACKAGE_PREFIX)

internal fun computePackageNamesWithParentPackages(packageNames: Set<String>): Set<String> {
    if (packageNames.isEmpty()) return emptySet()

    return buildSet {
        add("")

        packageNames.forEach { packageName ->
            var currentPackage = packageName
            while (currentPackage.isNotEmpty()) {
                add(currentPackage)
                currentPackage = currentPackage.substringBeforeLast(".", missingDelimiterValue = "")
            }
        }
    }
}

/**
 * Checks if this [FirBasedSymbol] has the given PSI element as a source.
 *
 * [hasPsi] exists to ensure a consistent approach to compare PSI in symbol providers, e.g. by [LLPsiAwareSymbolProvider].
 */
internal fun FirBasedSymbol<*>.hasPsi(element: PsiElement): Boolean = fir.psi == element

/**
 * Returns a [FirClassLikeSymbol] with the given [classId] that matches [declaration].
 *
 * If the symbol provider is not an [LLPsiAwareSymbolProvider], the function falls back to [FirSymbolProvider.getClassLikeSymbolByClassId],
 * but still ensures that the resulting symbol matches [declaration].
 */
@LLModuleSpecificSymbolProviderAccess
internal fun FirSymbolProvider.getClassLikeSymbolMatchingPsi(classId: ClassId, declaration: PsiElement): FirClassLikeSymbol<*>? {
    if (this is LLPsiAwareSymbolProvider) {
        return getClassLikeSymbolByPsi(classId, declaration)
    }

    return getClassLikeSymbolByClassId(classId)?.takeIf { symbol ->
        // If the symbol's PSI is `null`, it cannot be a symbol for `element`, since the PSI exists and any symbol created for it should
        // have a PSI source.
        symbol.hasPsi(declaration)
    }
}

internal fun FirSymbolProvider.getClassLikeSymbolByClassIdWithoutDependencies(classId: ClassId): FirClassLikeSymbol<*>? =
    when (this) {
        is LLModuleWithDependenciesSymbolProvider -> getClassLikeSymbolByClassIdWithoutDependencies(classId)
        else -> getClassLikeSymbolByClassId(classId)
    }

@LLModuleSpecificSymbolProviderAccess
internal fun FirSymbolProvider.getClassLikeSymbolByPsiWithoutDependencies(
    classId: ClassId,
    declaration: PsiElement,
): FirClassLikeSymbol<*>? =
    when (this) {
        is LLModuleWithDependenciesSymbolProvider -> getClassLikeSymbolByPsiWithoutDependencies(classId, declaration)
        else -> getClassLikeSymbolMatchingPsi(classId, declaration)
    }

internal fun FirSymbolProvider.getAllClassLikeSymbolsByClassIdOrSingle(classId: ClassId): List<FirClassLikeSymbol<*>> =
    when (this) {
        is LLMultiClassLikeSymbolProvider -> getAllClassLikeSymbolsByClassId(classId)
        else -> listOfNotNull(getClassLikeSymbolByClassId(classId))
    }

@LLModuleSpecificSymbolProviderAccess
internal fun LLPsiAwareSymbolProvider.getParentPsiClassSymbol(psiClass: PsiClass): FirRegularClassSymbol? =
    psiClass.containingClass?.let { getClassLikeSymbolByPsi(it.classIdOrError(), it) as? FirRegularClassSymbol }

internal fun FirJavaFacade.createPsiClassSymbol(
    psiClass: PsiClass,
    javaClass: JavaClass?,
    parentClassSymbol: FirRegularClassSymbol?,
): FirRegularClassSymbol {
    val classId = psiClass.classIdOrError()
    val symbol = FirRegularClassSymbol(classId)
    return convertJavaClassToFir(symbol, parentClassSymbol, javaClass ?: JavaClassImpl(psiClass)).symbol
}
