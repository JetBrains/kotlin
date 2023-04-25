/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirNameConflictsTracker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.util.ListMultimap
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartSet

internal class FirDefaultDeclarationPresenter : FirDeclarationPresenter

private val NO_NAME_PROVIDED = Name.special("<no name provided>")

// - see testEnumValuesValueOf.
// it generates a static function that has
// the same signature as the function defined
// explicitly.
// - see tests with `fun () {}`.
// you can't redeclare something that has no name.
private fun FirDeclaration.isCollectable() = when (this) {
    is FirSimpleFunction -> source?.kind !is KtFakeSourceElementKind && name != NO_NAME_PROVIDED
    is FirProperty -> source?.kind !is KtFakeSourceElementKind.EnumGeneratedDeclaration
    is FirRegularClass -> name != NO_NAME_PROVIDED
    else -> true
}

/**
 * Collects FirDeclarations for further analysis.
 */
class FirDeclarationInspector {
    val declarationConflictingSymbols: HashMap<FirDeclaration, SmartSet<FirBasedSymbol<*>>> = hashMapOf()
    private val presenter: FirDeclarationPresenter = FirDefaultDeclarationPresenter()
    private val otherDeclarations = mutableMapOf<String, MutableList<FirDeclaration>>()
    private val functionDeclarations = mutableMapOf<String, MutableList<FirSimpleFunction>>()

    fun collect(declaration: FirDeclaration) {
        when {
            !declaration.isCollectable() -> {}
            declaration is FirSimpleFunction -> collectFunction(presenter.represent(declaration), declaration)
            declaration is FirRegularClass -> collectNonFunctionDeclaration(presenter.represent(declaration), declaration)
            declaration is FirTypeAlias -> collectNonFunctionDeclaration(presenter.represent(declaration), declaration)
            declaration is FirVariable -> collectNonFunctionDeclaration(presenter.represent(declaration), declaration)
        }
    }

    fun collectWithExternalConflicts(
        declaration: FirDeclaration,
        containingFile: FirFile,
        session: FirSession,
        packageMemberScope: FirPackageMemberScope
    ) {
        collect(declaration)
        var declarationName: Name? = null
        val declarationPresentation = presenter.represent(declaration) ?: return

        when (declaration) {
            is FirSimpleFunction -> {
                declarationName = declaration.name
                if (!declarationName.isSpecial) {
                    packageMemberScope.processFunctionsByName(declarationName) {
                        collectExternalConflict(
                            declaration, declarationPresentation, containingFile, it, null, null, session
                        )
                    }
                    packageMemberScope.processClassifiersByNameWithSubstitution(declarationName) { symbol, _ ->
                        symbol.lazyResolveToPhase(FirResolvePhase.STATUS)
                        @OptIn(SymbolInternals::class)
                        val classWithSameName = symbol.fir as? FirRegularClass
                        classWithSameName?.onConstructors { constructor ->
                            collectExternalConflict(
                                declaration, declarationPresentation, containingFile,
                                constructor.symbol, presenter.represent(constructor, classWithSameName), null,
                                session
                            )
                        }
                    }
                }
            }
            is FirVariable -> {
                declarationName = declaration.name
                if (!declarationName.isSpecial) {
                    packageMemberScope.processPropertiesByName(declarationName) {
                        collectExternalConflict(
                            declaration, declarationPresentation, containingFile, it, null, null, session
                        )
                    }
                }
            }
            is FirRegularClass -> {
                declarationName = declaration.name

                if (!declarationName.isSpecial) {
                    packageMemberScope.processClassifiersByNameWithSubstitution(declarationName) { symbol, _ ->
                        collectExternalConflict(
                            declaration, declarationPresentation, containingFile, symbol, null, null, session
                        )
                    }
                    declaration.onConstructors { constructor ->
                        packageMemberScope.processFunctionsByName(declarationName!!) {
                            collectExternalConflict(
                                constructor, presenter.represent(constructor, declaration), containingFile,
                                it, null, null, session
                            )
                        }
                    }

                    session.nameConflictsTracker?.let { it as? FirNameConflictsTracker }
                        ?.redeclaredClassifiers?.get(declaration.symbol.classId)?.forEach {
                            collectExternalConflict(
                                declaration,
                                declarationPresentation,
                                containingFile,
                                it.classifier,
                                null,
                                it.file,
                                session
                            )
                        }
                }
            }
            is FirTypeAlias -> {
                declarationName = declaration.name
                if (!declarationName.isSpecial) {
                    packageMemberScope.processClassifiersByNameWithSubstitution(declarationName) { symbol, _ ->
                        collectExternalConflict(
                            declaration, declarationPresentation, containingFile, symbol, null, null, session
                        )
                    }
                    session.nameConflictsTracker?.let { it as? FirNameConflictsTracker }
                        ?.redeclaredClassifiers?.get(declaration.symbol.classId)?.forEach {
                            collectExternalConflict(
                                declaration,
                                declarationPresentation,
                                containingFile,
                                it.classifier,
                                null,
                                it.file,
                                session
                            )
                        }
                }
            }
            else -> {
            }
        }
        if (declarationName != null) {
            session.lookupTracker?.recordLookup(
                declarationName, containingFile.packageFqName.asString(), declaration.source, containingFile.source
            )
        }
    }

    private fun collectNonFunctionDeclaration(key: String, declaration: FirDeclaration): MutableList<FirDeclaration> =
        otherDeclarations.getOrPut(key) {
            mutableListOf()
        }.also {
            it.add(declaration)
            collectLocalConflicts(declaration, it)
        }

    private fun collectFunction(key: String, declaration: FirSimpleFunction): MutableList<FirSimpleFunction> =
        functionDeclarations.getOrPut(key) {
            mutableListOf()
        }.also {
            it.add(declaration)
            collectLocalConflicts(declaration, it)
        }

    private fun collectLocalConflicts(declaration: FirDeclaration, conflicting: List<FirDeclaration>) {
        val localConflicts = SmartSet.create<FirBasedSymbol<*>>()
        for (otherDeclaration in conflicting) {
            if (otherDeclaration is FirField && otherDeclaration.source?.kind == KtFakeSourceElementKind.ClassDelegationField) {
                // class delegation field will be renamed after by the IR backend in a case of a name clash
                continue
            }
            if (otherDeclaration != declaration && !isExpectAndActual(declaration, otherDeclaration)) {
                localConflicts.add(otherDeclaration.symbol)
                declarationConflictingSymbols.getOrPut(otherDeclaration) { SmartSet.create() }.add(declaration.symbol)
            }
        }
        declarationConflictingSymbols[declaration] = localConflicts
    }

    private fun isExpectAndActual(declaration1: FirDeclaration, declaration2: FirDeclaration): Boolean {
        if (declaration1 !is FirMemberDeclaration) return false
        if (declaration2 !is FirMemberDeclaration) return false
        return (declaration1.status.isExpect && declaration2.status.isActual) ||
                (declaration1.status.isActual && declaration2.status.isExpect)
    }

    private fun areCompatibleMainFunctions(
        declaration1: FirDeclaration, file1: FirFile, declaration2: FirDeclaration, file2: FirFile?
    ): Boolean {
        // TODO: proper main function detector
        if (declaration1 !is FirSimpleFunction || declaration2 !is FirSimpleFunction) return false
        if (declaration1.name.asString() != "main" || declaration2.name.asString() != "main") return false
        return file1 != file2
    }

    private fun collectExternalConflict(
        declaration: FirDeclaration,
        declarationPresentation: String,
        containingFile: FirFile,
        conflictingSymbol: FirBasedSymbol<*>,
        conflictingPresentation: String?,
        conflictingFile: FirFile?,
        session: FirSession
    ) {
        conflictingSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)
        @OptIn(SymbolInternals::class)
        val conflicting = conflictingSymbol.fir
        if (conflicting == declaration || declaration.moduleData != conflicting.moduleData) return
        val actualConflictingPresentation = conflictingPresentation ?: presenter.represent(conflicting)
        if (actualConflictingPresentation != declarationPresentation) return
        val actualConflictingFile =
            conflictingFile ?: when (conflictingSymbol) {
                is FirClassLikeSymbol<*> -> session.firProvider.getFirClassifierContainerFileIfAny(conflictingSymbol)
                is FirCallableSymbol<*> -> session.firProvider.getFirCallableContainerFile(conflictingSymbol)
                else -> null
            }
        if (containingFile == actualConflictingFile && conflicting.origin == FirDeclarationOrigin.Precompiled) {
            return // TODO: rewrite local decls checker to the same logic and then remove the check
        }
        if (areCompatibleMainFunctions(declaration, containingFile, conflicting, actualConflictingFile)) return
        if (isExpectAndActual(declaration, conflicting)) return
        if (
            conflicting is FirMemberDeclaration &&
            !session.visibilityChecker.isVisible(conflicting, session, containingFile, emptyList(), dispatchReceiver = null)
        ) return
        val declarationIsLowPriority = hasLowPriorityAnnotation(declaration.annotations)
        val conflictingIsLowPriority = hasLowPriorityAnnotation(conflicting.annotations)
        if (declarationIsLowPriority != conflictingIsLowPriority) return
        declarationConflictingSymbols.getOrPut(declaration) { SmartSet.create() }.add(conflictingSymbol)
    }

    private fun FirDeclarationPresenter.represent(declaration: FirDeclaration): String? =
        when (declaration) {
            is FirSimpleFunction -> represent(declaration)
            is FirRegularClass -> represent(declaration)
            is FirTypeAlias -> represent(declaration)
            is FirProperty -> represent(declaration)
            else -> null
        }

    private fun FirRegularClass.onConstructors(action: (ctor: FirConstructor) -> Unit) {
        class ClassConstructorVisitor : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {}

            override fun visitConstructor(constructor: FirConstructor) {
                action(constructor)
            }

            override fun visitDeclarationStatus(declarationStatus: FirDeclarationStatus) {}
            override fun visitRegularClass(regularClass: FirRegularClass) {}
            override fun visitProperty(property: FirProperty) {}
            override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {}
        }

        if (classKind != ClassKind.OBJECT && classKind != ClassKind.ENUM_ENTRY) {
            acceptChildren(ClassConstructorVisitor())
        }
    }
}

fun checkConflictingElements(elements: List<FirElement>, context: CheckerContext, reporter: DiagnosticReporter) {
    if (elements.size <= 1) return

    val multimap = ListMultimap<Name, FirBasedSymbol<*>>()
    for (element in elements) {
        val name: Name?
        val symbol: FirBasedSymbol<*>?
        when (element) {
            is FirVariable -> {
                symbol = element.symbol
                name = element.name
            }
            is FirOuterClassTypeParameterRef -> {
                continue
            }
            is FirTypeParameterRef -> {
                symbol = element.symbol
                name = symbol.name
            }
            else -> {
                symbol = null
                name = null
            }
        }
        if (name?.isSpecial == false) {
            multimap.put(name, symbol!!)
        }
    }
    for (key in multimap.keys) {
        val conflictingElements = multimap[key]
        if (conflictingElements.size > 1) {
            for (conflictingElement in conflictingElements) {
                reporter.reportOn(conflictingElement.source, FirErrors.REDECLARATION, conflictingElements, context)
            }
        }
    }
}
