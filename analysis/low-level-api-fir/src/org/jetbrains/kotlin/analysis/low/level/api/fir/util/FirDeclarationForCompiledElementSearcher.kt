/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.KtDeclarationAndFirDeclarationEqualityChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredConstructors
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredFunctionSymbols
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.jetbrains.kotlin.utils.withAttachmentBuilder

/**
 * Allows to search for FIR declarations by compiled [KtDeclaration]s.
 */
internal class FirDeclarationForCompiledElementSearcher(private val symbolProvider: FirSymbolProvider) {
    fun findNonLocalDeclaration(ktDeclaration: KtDeclaration): FirDeclaration {
        return when (ktDeclaration) {
            is KtEnumEntry -> findNonLocalEnumEntry(ktDeclaration)
            is KtClassLikeDeclaration -> findNonLocalClassLikeDeclaration(ktDeclaration)
            is KtConstructor<*> -> findConstructorOfNonLocalClass(ktDeclaration)
            is KtNamedFunction -> findNonLocalFunction(ktDeclaration)
            is KtProperty -> findNonLocalProperty(ktDeclaration)

            else -> firErrorWithAttachment("Unsupported compiled declaration of type", psi = ktDeclaration)
        }
    }

    private fun findNonLocalEnumEntry(declaration: KtEnumEntry): FirEnumEntry {
        require(!declaration.isLocal)
        val classId = declaration.containingClassOrObject?.getClassId()
            ?: firErrorWithAttachment("Non-local class should have classId", psi = declaration)

        val classCandidate = symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: firErrorWithAttachment("We should be able to find a symbol for $classId", psi = declaration)

        return (classCandidate.fir as? FirRegularClass)?.declarations?.first {
            it is FirEnumEntry && it.name == declaration.nameAsName
        } as FirEnumEntry
    }

    private fun findNonLocalClassLikeDeclaration(declaration: KtClassLikeDeclaration): FirClassLikeDeclaration {
        val classId = declaration.getClassId()
            ?: firErrorWithAttachment("Non-local class should have classId", psi = declaration)

        val classCandidate = symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: firErrorWithAttachment("We should be able to find a symbol for $classId", psi = declaration) {
                withAttachment("classId", classId.asString())
            }

        return classCandidate.fir
    }

    private fun findConstructorOfNonLocalClass(declaration: KtConstructor<*>): FirConstructor {
        val containingClass = declaration.containingClassOrObject
            ?: firErrorWithAttachment("Constructor must have outer class", psi = declaration)

        require(!containingClass.isLocal)
        val classId = containingClass.getClassId()
            ?: firErrorWithAttachment("Non-local class should have classId", psi = declaration)

        val constructorCandidate =
            symbolProvider.getClassDeclaredConstructors(classId)
                .singleOrNull { representSameConstructor(declaration, it.fir) }
                ?: firErrorWithAttachment("We should be able to find a constructor", psi = declaration)

        return constructorCandidate.fir
    }

    private fun findNonLocalFunction(declaration: KtNamedFunction): FirFunction {
        require(!declaration.isLocal)

        val candidates = symbolProvider.findFunctionCandidates(declaration)
        val functionCandidate =
            candidates
                .singleOrNull { KtDeclarationAndFirDeclarationEqualityChecker.representsTheSameDeclaration(declaration, it.fir) }
                ?: firErrorWithAttachment("We should be able to find a symbol for function", psi = declaration) {
                    withCandidates(candidates)
                }

        return functionCandidate.fir
    }


    private fun findNonLocalProperty(declaration: KtProperty): FirProperty {
        require(!declaration.isLocal)

        val candidates = symbolProvider.findPropertyCandidates(declaration)
        val propertyCandidate =
            candidates.singleOrNull { KtDeclarationAndFirDeclarationEqualityChecker.representsTheSameDeclaration(declaration, it.fir) }
                ?: firErrorWithAttachment("We should be able to find a symbol for property", psi = declaration) {
                    withCandidates(candidates)
                }

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
        ?: firErrorWithAttachment("No containing non-local declaration found for", psi = declaration)

    return getClassDeclaredFunctionSymbols(containerClassId, declaration.nameAsSafeName) +
            getClassDeclaredPropertySymbols(containerClassId, declaration.nameAsSafeName)
}

private fun representSameConstructor(psiConstructor: KtConstructor<*>, firConstructor: FirConstructor): Boolean {
    if ((firConstructor.isPrimary) != (psiConstructor is KtPrimaryConstructor)) {
        return false
    }

    return KtDeclarationAndFirDeclarationEqualityChecker.representsTheSameDeclaration(psiConstructor, firConstructor)
}

private fun KotlinExceptionWithAttachments.withCandidates(candidates: List<FirBasedSymbol<*>>) {
    withAttachmentBuilder("candidates") {
        appendLine("Candidates count ${candidates.size}")
        for ((index, candidate) in candidates.withIndex()) {
            appendLine("Candidate $index, ${candidate::class.java}")
            val ktModule = candidate.llFirModuleData.ktModule
            appendLine("From ${ktModule::class.java} ${ktModule.moduleDescription}")
            appendLine("Origin ${candidate.origin}")
            appendLine(debugFirRenderer.renderElementAsString(candidate.fir))
            appendLine()
        }
    }
}
