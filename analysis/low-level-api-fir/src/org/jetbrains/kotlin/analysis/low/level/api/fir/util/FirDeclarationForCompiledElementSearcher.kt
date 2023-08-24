/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.containingDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirBuiltinsAndCloneableSession
import org.jetbrains.kotlin.analysis.project.structure.KtBuiltinsModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.analysis.utils.errors.withClassEntry
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

/**
 * Allows to search for FIR declarations by compiled [KtDeclaration]s.
 */
internal class FirDeclarationForCompiledElementSearcher(private val symbolProvider: FirSymbolProvider) {
    private val projectStructureProvider by lazy {
        val project = symbolProvider.session.llFirModuleData.ktModule.project
        ProjectStructureProvider.getInstance(project)
    }

    fun findNonLocalDeclaration(ktDeclaration: KtDeclaration): FirDeclaration = when (ktDeclaration) {
        is KtEnumEntry -> findNonLocalEnumEntry(ktDeclaration)
        is KtClassLikeDeclaration -> findNonLocalClassLikeDeclaration(ktDeclaration)
        is KtConstructor<*> -> findConstructorOfNonLocalClass(ktDeclaration)
        is KtNamedFunction -> findNonLocalFunction(ktDeclaration)
        is KtProperty -> findNonLocalProperty(ktDeclaration)
        is KtParameter -> findParameter(ktDeclaration)
        is KtPropertyAccessor -> findNonLocalPropertyAccessor(ktDeclaration)
        is KtTypeParameter -> findNonLocalTypeParameter(ktDeclaration)

        else -> errorWithFirSpecificEntries("Unsupported compiled declaration of type", psi = ktDeclaration)
    }

    private fun FirSymbolProvider.findFunctionCandidates(function: KtNamedFunction): List<FirFunctionSymbol<*>> =
        findCallableCandidates(function, function.isTopLevel).filterIsInstance<FirFunctionSymbol<*>>()

    private fun FirSymbolProvider.findPropertyCandidates(property: KtProperty): List<FirPropertySymbol> =
        findCallableCandidates(property, property.isTopLevel).filterIsInstance<FirPropertySymbol>()

    private fun FirSymbolProvider.findCallableCandidates(
        declaration: KtCallableDeclaration,
        isTopLevel: Boolean,
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

        val containingClass = declaration.containingClassOrObject?.let(::findNonLocalClassLikeDeclaration)
            ?: errorWithFirSpecificEntries("No containing non-local declaration found for", psi = declaration)

        val scope = session.declaredMemberScope(containingClass as FirClass, memberRequiredPhase = null)
        return when (declaration) {
            is KtProperty -> scope.getProperties(shortName)
            is KtNamedFunction -> scope.getFunctions(shortName)
            else -> errorWithFirSpecificEntries("Unexpected callable ${declaration::class.simpleName}") {
                withEntry("isTopLevel", isTopLevel.toString())
                withPsiEntry("declaration", declaration)
            }
        }
    }

    private fun findNonLocalTypeParameter(param: KtTypeParameter): FirDeclaration {
        val owner = param.containingDeclaration ?: errorWithFirSpecificEntries("Unsupported compiled type parameter", psi = param)
        val firDeclaration = findNonLocalDeclaration(owner)
        val firTypeParameterRefOwner = firDeclaration as? FirTypeParameterRefsOwner ?: errorWithFirSpecificEntries(
            "No fir found by $owner",
            psi = owner,
            fir = firDeclaration,
        )

        return firTypeParameterRefOwner.typeParameters.find { it.realPsi === param } as FirDeclaration
    }

    private fun findParameter(param: KtParameter): FirDeclaration {
        val ownerFunction = param.ownerFunction ?: errorWithFirSpecificEntries("Unsupported compiled parameter", psi = param)
        val firDeclaration = findNonLocalDeclaration(ownerFunction)
        val firFunction = firDeclaration as? FirFunction ?: errorWithFirSpecificEntries(
            "No fir function found by ktFunction",
            psi = ownerFunction,
            fir = firDeclaration
        )
        return firFunction.valueParameters.find { it.realPsi === param }
            ?: errorWithFirSpecificEntries("No fir value parameter found", psi = param, fir = firFunction)
    }

    private fun findNonLocalEnumEntry(declaration: KtEnumEntry): FirEnumEntry {
        val classCandidate = declaration.containingClassOrObject?.let(::findNonLocalClassLikeDeclaration)
            ?: errorWithFirSpecificEntries("Enum entry must have containing class", psi = declaration)

        return (classCandidate as FirRegularClass).declarations.first {
            it is FirEnumEntry && it.realPsi === declaration
        } as FirEnumEntry
    }

    private fun findNonLocalClassLikeDeclaration(declaration: KtClassLikeDeclaration): FirClassLikeDeclaration {
        val classId = declaration.getClassId() ?: errorWithFirSpecificEntries("Non-local class should have classId", psi = declaration)

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

        val containingFirClass = findNonLocalClassLikeDeclaration(containingClass) as FirClass
        val constructorCandidate = containingFirClass.constructors(symbolProvider.session).singleOrNull { it.fir.realPsi === declaration }
            ?: errorWithFirSpecificEntries("We should be able to find a constructor", psi = declaration, fir = containingFirClass)

        return constructorCandidate.fir
    }

    private fun findNonLocalFunction(declaration: KtNamedFunction): FirFunction {
        require(!declaration.isLocal)

        val candidates = symbolProvider.findFunctionCandidates(declaration)
        val functionCandidate = candidates.firstOrNull { it.fir.realPsi === declaration }
            ?: errorWithFirSpecificEntries("We should be able to find a symbol for function", psi = declaration) {
                withCandidates(candidates)
            }

        return functionCandidate.fir
    }


    private fun findNonLocalProperty(declaration: KtProperty): FirProperty {
        require(!declaration.isLocal)

        val candidates = symbolProvider.findPropertyCandidates(declaration)
        val propertyCandidate = candidates.firstOrNull { it.fir.realPsi === declaration }
            ?: errorWithFirSpecificEntries("We should be able to find a symbol for property", psi = declaration) {
                withCandidates(candidates)
            }

        return propertyCandidate.fir
    }

    private fun findNonLocalPropertyAccessor(declaration: KtPropertyAccessor): FirPropertyAccessor {
        val firProperty = findNonLocalProperty(declaration.property)

        return (if (declaration.isGetter) firProperty.getter else firProperty.setter)
            ?: errorWithFirSpecificEntries("We should be able to find a symbol for property accessor", psi = declaration)
    }

}

// Returns a built-in provider for a Kotlin standard library, as built-in declarations are its logical part.
// Returns one for built-ins modules as well, as these modules have empty scope and their content comes from the dependency provider.
private val LLFirModuleWithDependenciesSymbolProvider.friendBuiltinsProvider: FirSymbolProvider?
    get() {
        val moduleData = this.session.moduleData
        if (getPackageWithoutDependencies(StandardClassIds.BASE_KOTLIN_PACKAGE) != null
            || moduleData is LLFirModuleData && moduleData.ktModule is KtBuiltinsModule
        ) {
            return dependencyProvider.providers.find { it.session is LLFirBuiltinsAndCloneableSession }
        }

        return null
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
