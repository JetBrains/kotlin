/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirBuiltinsAndCloneableSession
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.utils.errors.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.analysis.utils.errors.withClassEntry
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

/**
 * Allows to search for FIR declarations by compiled [KtDeclaration]s.
 */
internal class FirDeclarationForCompiledElementSearcher(private val symbolProvider: FirSymbolProvider) {
    private val projectStructureProvider by lazy {
        val project = symbolProvider.session.llFirModuleData.ktModule.project
        ProjectStructureProvider.getInstance(project)
    }

    fun findNonLocalDeclaration(ktDeclaration: KtDeclaration): FirDeclaration {
        return when (ktDeclaration) {
            is KtEnumEntry -> findNonLocalEnumEntry(ktDeclaration)
            is KtClassLikeDeclaration -> findNonLocalClassLikeDeclaration(ktDeclaration)
            is KtConstructor<*> -> findConstructorOfNonLocalClass(ktDeclaration)
            is KtNamedFunction -> findNonLocalFunction(ktDeclaration)
            is KtProperty -> findNonLocalProperty(ktDeclaration)
            is KtParameter -> findParameter(ktDeclaration)

            else -> errorWithFirSpecificEntries("Unsupported compiled declaration of type", psi = ktDeclaration)
        }
    }

    private fun findParameter(param: KtParameter): FirDeclaration {
        val ownerFunction = param.ownerFunction ?: errorWithFirSpecificEntries("Unsupported compiled parameter", psi = param)
        val firDeclaration = findNonLocalDeclaration(ownerFunction)
        val firFunction = firDeclaration as? FirFunction ?: errorWithFirSpecificEntries(
            "No fir function found by ktFunction",
            psi = ownerFunction,
            fir = firDeclaration
        )
        return firFunction.valueParameters.find { it.name == param.nameAsSafeName }
            ?: errorWithFirSpecificEntries("No fir value parameter found", psi = param, fir = firFunction)
    }

    private fun findNonLocalEnumEntry(declaration: KtEnumEntry): FirEnumEntry {
        require(!declaration.isLocal)
        val classId = declaration.containingClassOrObject?.getClassId()
            ?: errorWithFirSpecificEntries("Non-local class should have classId", psi = declaration)

        val classCandidate = symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: errorWithFirSpecificEntries("We should be able to find a symbol for $classId", psi = declaration)

        return (classCandidate.fir as? FirRegularClass)?.declarations?.first {
            it is FirEnumEntry && it.name == declaration.nameAsName
        } as FirEnumEntry
    }

    private fun findNonLocalClassLikeDeclaration(declaration: KtClassLikeDeclaration): FirClassLikeDeclaration {
        val classId = declaration.getClassId()
            ?: errorWithFirSpecificEntries("Non-local class should have classId", psi = declaration)

        val classCandidate = when (symbolProvider) {
            is LLFirModuleWithDependenciesSymbolProvider -> {
                symbolProvider.getDeserializedClassLikeSymbolByClassIdWithoutDependencies(classId, declaration)
                    ?: symbolProvider.friendBuiltinsProvider?.getClassLikeSymbolByClassId(classId)
            }
            else -> {
                symbolProvider.getClassLikeSymbolByClassId(classId)
            }
        }

        if (classCandidate == null) {
            errorWithFirSpecificEntries("We should be able to find a symbol for $classId", psi = declaration) {
                withEntry("classId", classId) { it.asString() }

                val contextualModule = symbolProvider.session.llFirModuleData.ktModule
                val moduleForFile = projectStructureProvider.getModule(declaration, contextualModule)
                withEntry("ktModule", moduleForFile) { it.moduleDescription }
            }
        }

        return classCandidate.fir
    }

    private fun findConstructorOfNonLocalClass(declaration: KtConstructor<*>): FirConstructor {
        val containingClass = declaration.containingClassOrObject
            ?: errorWithFirSpecificEntries("Constructor must have outer class", psi = declaration)

        require(!containingClass.isLocal)
        val classId = containingClass.getClassId()
            ?: errorWithFirSpecificEntries("Non-local class should have classId", psi = declaration)

        val constructorCandidate =
            symbolProvider.getClassDeclaredConstructors(classId)
                .singleOrNull { representSameConstructor(declaration, it.fir) }
                ?: errorWithFirSpecificEntries("We should be able to find a constructor", psi = declaration)

        return constructorCandidate.fir
    }

    private fun findNonLocalFunction(declaration: KtNamedFunction): FirFunction {
        require(!declaration.isLocal)

        val candidates = symbolProvider.findFunctionCandidates(declaration)
        val functionCandidate =
            candidates
                .firstOrNull { it.fir.realPsi === declaration }
                ?: errorWithFirSpecificEntries("We should be able to find a symbol for function", psi = declaration) {
                    withCandidates(candidates)
                }

        return functionCandidate.fir
    }


    private fun findNonLocalProperty(declaration: KtProperty): FirProperty {
        require(!declaration.isLocal)

        val candidates = symbolProvider.findPropertyCandidates(declaration)
        val propertyCandidate =
            candidates.firstOrNull { it.fir.realPsi === declaration }
                ?: errorWithFirSpecificEntries("We should be able to find a symbol for property", psi = declaration) {
                    withCandidates(candidates)
                }

        return propertyCandidate.fir
    }

}

// Returns a built-in provider for a Kotlin standard library, as built-in declarations are its logical part.
private val LLFirModuleWithDependenciesSymbolProvider.friendBuiltinsProvider: FirSymbolProvider?
    get() {
        if (getPackageWithoutDependencies(StandardClassIds.BASE_KOTLIN_PACKAGE) != null) {
            return dependencyProvider.providers.find { it.session is LLFirBuiltinsAndCloneableSession }
        }

        return null
    }

private fun FirSymbolProvider.findFunctionCandidates(function: KtNamedFunction): List<FirFunctionSymbol<*>> =
    findCallableCandidates(function, function.isTopLevel).filterIsInstance<FirFunctionSymbol<*>>()

private fun FirSymbolProvider.findPropertyCandidates(property: KtProperty): List<FirPropertySymbol> =
    findCallableCandidates(property, property.isTopLevel).filterIsInstance<FirPropertySymbol>()

private fun FirSymbolProvider.findCallableCandidates(
    declaration: KtCallableDeclaration,
    isTopLevel: Boolean
): List<FirCallableSymbol<*>> {
    val shortName = declaration.nameAsSafeName

    if (isTopLevel) {
        val packageFqName = declaration.containingKtFile.packageFqName

        @OptIn(FirSymbolProviderInternals::class)
        return when (this) {
            is LLFirModuleWithDependenciesSymbolProvider -> buildList {
                getTopLevelDeserializedCallableSymbolsToWithoutDependencies(this, packageFqName, shortName, declaration)
                friendBuiltinsProvider?.getTopLevelCallableSymbolsTo(this, packageFqName, shortName)
            }
            else -> getTopLevelCallableSymbols(packageFqName, shortName)
        }
    }

    val containerClassId = declaration.containingClassOrObject?.getClassId()
        ?: errorWithFirSpecificEntries("No containing non-local declaration found for", psi = declaration)

    return getClassDeclaredFunctionSymbols(containerClassId, shortName) +
            getClassDeclaredPropertySymbols(containerClassId, shortName)
}

private fun representSameConstructor(psiConstructor: KtConstructor<*>, firConstructor: FirConstructor): Boolean {
    if ((firConstructor.isPrimary) != (psiConstructor is KtPrimaryConstructor)) {
        return false
    }

    return firConstructor.realPsi === psiConstructor
}

private fun ExceptionAttachmentBuilder.withCandidates(candidates: List<FirBasedSymbol<*>>) {
    withEntry("Candidates count", candidates.size.toString())
    for ((index, candidate) in candidates.withIndex()) {
        val ktModule = candidate.llFirModuleData.ktModule
        withEntryGroup(index.toString()) {
            withClassEntry("candidateClass", candidate)
            withEntry("module", ktModule) { it.moduleDescription }
            withEntry("origin", candidate.origin.toString())
            withFirEntry("candidateFir", candidate.fir)

        }
    }
}
