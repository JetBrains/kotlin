/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.utils.errors.withClassEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLFirElementByPsiElementChooser
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.containingDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.LLMismatchedPsiFirElementByPsiElementChooser
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirBuiltinsAndCloneableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
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

    /**
     * Workaround for KT-75256 in IntelliJ 2025.2: If we cannot find a function/property symbol with the regular
     * [firElementByPsiElementChooser], we choose a candidate based on the signature alone, ignoring the PSI. In other words, the requested
     * PSI element and the FIR element's PSI are allowed to mismatch.
     *
     * This solves a problem where [findNonLocalClassLikeDeclaration] returns a FIR element with mismatched PSI, and we cannot find a member
     * function/property with the requested PSI. For the workaround, it's better to return something than throw an exception.
     *
     * The workaround also has to be extended to other declarations which derive from the class. For example, we have to pick value
     * parameters by signature as well since the wrong FIR class can lead to the wrong FIR function, and then we cannot pick the right
     * parameter by PSI alone.
     */
    private val mismatchedPsiFirElementChooser = LLMismatchedPsiFirElementByPsiElementChooser()

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

            @OptIn(FirSymbolProviderInternals::class)
            return when (val symbolProvider = session.symbolProvider) {
                is LLModuleWithDependenciesSymbolProvider -> buildList {
                    symbolProvider.getTopLevelDeserializedCallableSymbolsToWithoutDependencies(this, packageFqName, shortName, declaration)
                    symbolProvider.friendBuiltinsProvider?.getTopLevelCallableSymbolsTo(this, packageFqName, shortName)
                }
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

        val typeParameters = firTypeParameterRefOwner.typeParameters
        val typeParameter =
            typeParameters.firstOrNull { firElementByPsiElementChooser.isMatchingTypeParameter(param, it.symbol.fir) }
                ?: typeParameters.firstOrNull { mismatchedPsiFirElementChooser.isMatchingTypeParameter(param, it.symbol.fir) }
                ?: errorWithFirSpecificEntries("No fir type parameter found", psi = param) {
                    withCandidates(typeParameters.map { it.symbol })
                }

        return typeParameter as FirDeclaration
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
                ?: firCallable.contextParameters.find { mismatchedPsiFirElementChooser.isMatchingValueParameter(param, it) }
                ?: errorWithFirSpecificEntries("No fir value parameter found", psi = param, fir = firCallable)
        } else {
            val firFunction = firDeclaration as? FirFunction ?: errorWithFirSpecificEntries(
                "${FirFunction::class.simpleName} expected but ${firDeclaration::class.simpleName} found",
                psi = ownerDeclaration,
                fir = firDeclaration,
            )

            firFunction.valueParameters.find { firElementByPsiElementChooser.isMatchingValueParameter(param, it) }
                ?: firFunction.valueParameters.find { mismatchedPsiFirElementChooser.isMatchingValueParameter(param, it) }
                ?: errorWithFirSpecificEntries("No fir value parameter found", psi = param, fir = firFunction)
        }
    }

    private fun findNonLocalEnumEntry(declaration: KtEnumEntry): FirEnumEntry {
        val classCandidate = declaration.containingClassOrObject?.let(::findNonLocalClassLikeDeclaration)
            ?: errorWithFirSpecificEntries("Enum entry must have containing class", psi = declaration)

        val declarations = (classCandidate as FirRegularClass).declarations
        val enumEntry =
            declarations.firstOrNull { it is FirEnumEntry && firElementByPsiElementChooser.isMatchingEnumEntry(declaration, it) }
                ?: declarations.firstOrNull { it is FirEnumEntry && mismatchedPsiFirElementChooser.isMatchingEnumEntry(declaration, it) }
                ?: errorWithFirSpecificEntries("We should be able to find an enum entry", psi = declaration) {
                    withCandidates(declarations.filterIsInstance<FirEnumEntry>().map { it.symbol })
                }

        return enumEntry as FirEnumEntry
    }

    private fun findNonLocalClassLikeDeclaration(declaration: KtClassLikeDeclaration): FirClassLikeDeclaration {
        val classId = declaration.getClassId() ?: errorWithFirSpecificEntries("Non-local class should have classId", psi = declaration)

        val classCandidate = when (val symbolProvider = session.symbolProvider) {
            is LLModuleWithDependenciesSymbolProvider -> {
                symbolProvider.getDeserializedClassLikeSymbolByClassIdWithoutDependencies(classId, declaration)
                    ?: symbolProvider.friendBuiltinsProvider?.getClassLikeSymbolByClassId(classId)
            }
            else -> {
                symbolProvider.getClassLikeSymbolByClassId(classId)
            }
        }

        if (
            classCandidate != null &&
            firElementByPsiElementChooser.isMatchingClassLikeDeclaration(classId, declaration, classCandidate.fir)
        ) {
            return classCandidate.fir
        }

        if (classCandidate != null) {
            // In many cases, analysis still works if we return the wrong candidate FIR declaration, since it's likely to have the same
            // shape as the actual sought declaration. We don't log an error here because we already have enough data to implement KT-72998
            // and want to avoid spamming our users with errors.
            return classCandidate.fir
        } else {
            @Suppress("SENSELESS_COMPARISON")
            errorWithFirSpecificEntries(
                "We should be able to find a symbol for $classId (has candidate: ${classCandidate != null})",
                psi = declaration,
            ) {
                withEntry("classId", classId) { it.asString() }
                withPsiEntry("candidatePsi", classCandidate?.fir?.psi)
                withFirEntry("candidateFir", classCandidate?.fir)

                val contextualModule = session.llFirModuleData.ktModule
                val moduleForFile = projectStructureProvider.getModule(declaration, contextualModule)
                withEntry("ktModule", moduleForFile) { it.moduleDescription }
            }
        }
    }

    private fun findConstructorOfNonLocalClass(declaration: KtConstructor<*>): FirConstructor {
        val containingClass = declaration.containingClassOrObject
            ?: errorWithFirSpecificEntries("Constructor must have outer class", psi = declaration)

        val containingFirClass = findNonLocalClassLikeDeclaration(containingClass) as FirClass
        val constructors = containingFirClass.constructors(session)
        val constructorCandidate =
            constructors.singleOrNull { firElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.fir) }
                ?: constructors.singleOrNull { mismatchedPsiFirElementChooser.isMatchingCallableDeclaration(declaration, it.fir) }
                ?: errorWithFirSpecificEntries("We should be able to find a constructor", psi = declaration, fir = containingFirClass)

        return constructorCandidate.fir
    }

    private fun findNonLocalFunction(declaration: KtNamedFunction): FirFunction {
        require(!declaration.isLocal)

        val candidates = findFunctionCandidates(declaration)
        val functionCandidate =
            candidates.firstOrNull { firElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.fir) }
                ?: candidates.firstOrNull { mismatchedPsiFirElementChooser.isMatchingCallableDeclaration(declaration, it.fir) }
                ?: errorWithFirSpecificEntries("We should be able to find a symbol for function", psi = declaration) {
                    withCandidates(candidates)
                }

        return functionCandidate.fir
    }

    private fun findNonLocalProperty(declaration: KtProperty): FirProperty {
        require(!declaration.isLocal)

        val candidates = findPropertyCandidates(declaration)
        val propertyCandidate =
            candidates.firstOrNull { firElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.fir) }
                ?: candidates.firstOrNull { mismatchedPsiFirElementChooser.isMatchingCallableDeclaration(declaration, it.fir) }
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
private val LLModuleWithDependenciesSymbolProvider.friendBuiltinsProvider: FirSymbolProvider?
    get() {
        val moduleData = this.session.moduleData
        if (hasPackageWithoutDependencies(StandardClassIds.BASE_KOTLIN_PACKAGE)
            || moduleData is LLFirModuleData && moduleData.ktModule is KaBuiltinsModule
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
