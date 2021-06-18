/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredConstructors
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredFunctionSymbols
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.api.KtDeclarationAndFirDeclarationEqualityChecker
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

/**
 * Allows to search for FIR declarations by compiled [KtDeclaration]s.
 */
internal class FirDeclarationForCompiledElementSearcher(private val symbolProvider: FirSymbolProvider) {
    fun findNonLocalClass(declaration: KtClassOrObject): FirClassLikeDeclaration<*> {
        require(!declaration.isLocal)
        val classId = declaration.getClassId()
            ?: error("Non-local class should have classId. The class is ${declaration.getElementTextInContext()}")

        val classCandidate = symbolProvider.getClassLikeSymbolByFqName(classId)
            ?: error("We should be able to find a symbol for $classId")

        return classCandidate.fir
    }

    fun findConstructorOfNonLocalClass(declaration: KtConstructor<*>): FirConstructor {
        val containingClass = declaration.containingClassOrObject
            ?: error("Constructor must have outer class: ${declaration.getElementTextInContext()}")

        require(!containingClass.isLocal)
        val classId = containingClass.getClassId()
            ?: error("Non-local class should have classId. The class is ${containingClass.getElementTextInContext()}")

        val constructorCandidate =
            symbolProvider.getClassDeclaredConstructors(classId)
                .singleOrNull { representSameConstructor(declaration, it.fir) }
                ?: error("We should be able to find a constructor: ${declaration.getElementTextInContext()}")

        return constructorCandidate.fir
    }

    fun findNonLocalFunction(declaration: KtNamedFunction): FirFunction<*> {
        require(!declaration.isLocal)

        val functionCandidate =
            symbolProvider.findFunctionCandidates(declaration)
                .singleOrNull { representSameFunction(declaration, it.fir) }
                ?: error("We should be able to find a symbol for function ${declaration.name}: ${declaration.getElementTextInContext()}")

        return functionCandidate.fir
    }

    fun findNonLocalProperty(declaration: KtProperty): FirProperty {
        require(!declaration.isLocal)

        val propertyCandidate =
            symbolProvider.findPropertyCandidates(declaration)
                .singleOrNull()
                ?: error("We should be able to find a symbol for property ${declaration.name}: ${declaration.getElementTextInContext()}")

        return propertyCandidate.fir
    }

}

private fun FirSymbolProvider.findFunctionCandidates(function: KtNamedFunction): List<FirFunctionSymbol<*>> =
    findCallableCandidates(function, function.isTopLevel).filterIsInstance<FirFunctionSymbol<*>>()

private fun FirSymbolProvider.findPropertyCandidates(property: KtProperty): List<FirPropertySymbol> =
    findCallableCandidates(property, property.isTopLevel).filterIsInstance<FirPropertySymbol>()

private fun FirSymbolProvider.findCallableCandidates(
    declaration: KtCallableDeclaration,
    isTopLevel: Boolean
): List<FirCallableSymbol<*>> {
    if (isTopLevel) {
        return getTopLevelCallableSymbols(declaration.containingKtFile.packageFqName, declaration.nameAsSafeName)
    }

    val containerClassId = declaration.containingClassOrObject?.getClassId()
        ?: error("No containing non-local declaration found for ${declaration.getElementTextInContext()}")

    return getClassDeclaredFunctionSymbols(containerClassId, declaration.nameAsSafeName) +
            getClassDeclaredPropertySymbols(containerClassId, declaration.nameAsSafeName)
}

private fun representSameFunction(psiFunction: KtNamedFunction, it: FirFunction<*>): Boolean =
    KtDeclarationAndFirDeclarationEqualityChecker.representsTheSameDeclaration(psiFunction, it)

private fun representSameConstructor(psiConstructor: KtConstructor<*>, firConstructor: FirConstructor): Boolean {
    if ((firConstructor.isPrimary) != (psiConstructor is KtPrimaryConstructor)) {
        return false
    }

    return KtDeclarationAndFirDeclarationEqualityChecker.representsTheSameDeclaration(psiConstructor, firConstructor)
}
