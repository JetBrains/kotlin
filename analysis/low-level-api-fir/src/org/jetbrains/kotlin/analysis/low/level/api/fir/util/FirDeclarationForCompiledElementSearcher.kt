/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.utils.errors.withClassEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLFirElementByPsiElementChooser
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.containingDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLModuleSpecificSymbolProviderAccess
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.getClassLikeSymbolByClassIdWithoutDependencies
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.getClassLikeSymbolByPsiWithoutDependencies
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

/**
 * Allows to search for FIR declarations by compiled [KtDeclaration]s.
 */
internal class FirDeclarationForCompiledElementSearcher(private val session: LLFirSession) {
    private val project get() = session.project

    private val projectStructureProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinProjectStructureProvider.getInstance(project)
    }

    private val firElementByPsiElementChooser by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLFirElementByPsiElementChooser.getInstance(project)
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

    private fun findFunctionCandidates(function: KtNamedFunction): List<FirFunctionSymbol<*>> =
        findCallableCandidates(function, function.isTopLevel).filterIsInstance<FirFunctionSymbol<*>>()

    private fun findPropertyCandidates(property: KtProperty): List<FirPropertySymbol> =
        findCallableCandidates(property, property.isTopLevel).filterIsInstance<FirPropertySymbol>()

    private fun findCallableCandidates(
        declaration: KtCallableDeclaration,
        isTopLevel: Boolean,
    ): List<FirCallableSymbol<*>> {
        val shortName = declaration.nameAsSafeName

        if (isTopLevel) {
            val packageFqName = declaration.containingKtFile.packageFqName

            return when (val symbolProvider = session.symbolProvider) {
                is LLModuleWithDependenciesSymbolProvider ->
                    symbolProvider.getTopLevelDeserializedCallableSymbolsWithoutDependencies(packageFqName, shortName, declaration)

                else -> symbolProvider.getTopLevelCallableSymbols(packageFqName, shortName)
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

        return firTypeParameterRefOwner.typeParameters.find { typeParameterRef ->
            firElementByPsiElementChooser.isMatchingTypeParameter(param, typeParameterRef.symbol.fir)
        } as FirDeclaration
    }

    private fun findParameter(param: KtParameter): FirDeclaration {
        val ownerDeclaration = param.ownerDeclaration ?: errorWithFirSpecificEntries("Unsupported compiled parameter", psi = param)
        val firDeclaration = findNonLocalDeclaration(ownerDeclaration)
        return if (param.isContextParameter) {
            val firCallable = firDeclaration as? FirCallableDeclaration ?: errorWithFirSpecificEntries(
                "${FirCallableDeclaration::class.simpleName} expected but ${firDeclaration::class.simpleName} found",
                psi = ownerDeclaration,
                fir = firDeclaration,
            )

            firCallable.contextParameters.find { firElementByPsiElementChooser.isMatchingValueParameter(param, it) }
                ?: errorWithFirSpecificEntries("No fir value parameter found", psi = param, fir = firCallable)
        } else {
            val firFunction = firDeclaration as? FirFunction ?: errorWithFirSpecificEntries(
                "${FirFunction::class.simpleName} expected but ${firDeclaration::class.simpleName} found",
                psi = ownerDeclaration,
                fir = firDeclaration,
            )

            firFunction.valueParameters.find { firElementByPsiElementChooser.isMatchingValueParameter(param, it) }
                ?: errorWithFirSpecificEntries("No fir value parameter found", psi = param, fir = firFunction)
        }
    }

    private fun findNonLocalEnumEntry(declaration: KtEnumEntry): FirEnumEntry {
        val classCandidate = declaration.containingClassOrObject?.let(::findNonLocalClassLikeDeclaration)
            ?: errorWithFirSpecificEntries("Enum entry must have containing class", psi = declaration)

        return (classCandidate as FirRegularClass).declarations.first {
            it is FirEnumEntry && firElementByPsiElementChooser.isMatchingEnumEntry(declaration, it)
        } as FirEnumEntry
    }

    private fun findNonLocalClassLikeDeclaration(declaration: KtClassLikeDeclaration): FirClassLikeDeclaration {
        val classId = declaration.getClassId() ?: errorWithFirSpecificEntries("Non-local class should have classId", psi = declaration)

        // With the `BINARIES` origin, deserialized FIR declarations don't have associated PSI elements. Hence, we cannot use `*ByPsi*
        // functions, as they check the candidate's associated PSI.
        val classLikeSymbol = when (KotlinPlatformSettings.getInstance(project).deserializedDeclarationsOrigin) {
            KotlinDeserializedDeclarationsOrigin.BINARIES -> findBinaryClassLikeSymbol(classId)
            KotlinDeserializedDeclarationsOrigin.STUBS -> findStubClassLikeSymbol(classId, declaration)
        }

        classLikeSymbol?.let { return it.fir }

        errorWithFirSpecificEntries(
            "We should be able to find a symbol for class-like declaration",
            psi = declaration,
        ) {
            withEntry("classId", classId) { it.asString() }

            val contextualModule = session.llFirModuleData.ktModule
            val moduleForFile = projectStructureProvider.getModule(declaration, contextualModule)
            withEntry("ktModule", moduleForFile) { it.moduleDescription }
        }
    }

    private fun findBinaryClassLikeSymbol(classId: ClassId): FirClassLikeSymbol<*>? =
        session.symbolProvider.getClassLikeSymbolByClassIdWithoutDependencies(classId)

    /**
     * Note regarding [LLModuleSpecificSymbolProviderAccess]: [FirDeclarationForCompiledElementSearcher] must be queried with PSI elements
     * that are contained in the compiled element searcher's module. As such, it's also legal to call module-specific symbol provider
     * functions on that module's symbol provider.
     */
    @OptIn(LLModuleSpecificSymbolProviderAccess::class)
    private fun findStubClassLikeSymbol(classId: ClassId, declaration: KtClassLikeDeclaration): FirClassLikeSymbol<*>? =
        session.symbolProvider.getClassLikeSymbolByPsiWithoutDependencies(classId, declaration)

    private fun findConstructorOfNonLocalClass(declaration: KtConstructor<*>): FirConstructor {
        val containingClass = declaration.containingClassOrObject
            ?: errorWithFirSpecificEntries("Constructor must have outer class", psi = declaration)

        val containingFirClass = findNonLocalClassLikeDeclaration(containingClass) as FirClass
        val constructorCandidate = containingFirClass.constructors(session)
            .singleOrNull { firElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.fir) }
            ?: errorWithFirSpecificEntries("We should be able to find a constructor", psi = declaration, fir = containingFirClass)

        return constructorCandidate.fir
    }

    private fun findNonLocalFunction(declaration: KtNamedFunction): FirFunction {
        require(!declaration.isLocal)

        val candidates = findFunctionCandidates(declaration)
        val functionCandidate = candidates.firstOrNull { firElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.fir) }
            ?: errorWithFirSpecificEntries("We should be able to find a symbol for function", psi = declaration) {
                withCandidates(candidates)
            }

        return functionCandidate.fir
    }

    private fun findNonLocalProperty(declaration: KtProperty): FirProperty {
        require(!declaration.isLocal)

        val candidates = findPropertyCandidates(declaration)
        val propertyCandidate = candidates.firstOrNull { firElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.fir) }
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
