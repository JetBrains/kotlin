/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.FirDeclarationInspector
import org.jetbrains.kotlin.fir.analysis.checkers.FirDeclarationPresenter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.PACKAGE_MEMBER
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.util.ListMultimap
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartSet

object FirConflictsChecker : FirBasicDeclarationChecker() {

    private class DeclarationInspector : FirDeclarationInspector() {

        val declarationConflictingSymbols: HashMap<FirDeclaration, SmartSet<FirBasedSymbol<*>>> = hashMapOf()

        override fun collectNonFunctionDeclaration(key: String, declaration: FirDeclaration): MutableList<FirDeclaration> =
            super.collectNonFunctionDeclaration(key, declaration).also {
                collectLocalConflicts(declaration, it)
            }

        override fun collectFunction(key: String, declaration: FirSimpleFunction): MutableList<FirSimpleFunction> =
            super.collectFunction(key, declaration).also {
                collectLocalConflicts(declaration, it)
            }

        private fun collectLocalConflicts(declaration: FirDeclaration, conflicting: List<FirDeclaration>) {
            val localConflicts = SmartSet.create<FirBasedSymbol<*>>()
            for (otherDeclaration in conflicting) {
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
            conflictingSymbol.ensureResolved(FirResolvePhase.STATUS)
            @OptIn(SymbolInternals::class)
            val conflicting = conflictingSymbol.fir
            if (declaration.moduleData != conflicting.moduleData) return
            val actualConflictingPresentation = conflictingPresentation ?: presenter.represent(conflicting)
            if (conflicting == declaration || actualConflictingPresentation != declarationPresentation) return
            val actualConflictingFile =
                conflictingFile ?: when (conflictingSymbol) {
                    is FirClassLikeSymbol<*> -> session.firProvider.getFirClassifierContainerFileIfAny(conflictingSymbol)
                    is FirCallableSymbol<*> -> session.firProvider.getFirCallableContainerFile(conflictingSymbol)
                    else -> null
                }
            if (containingFile == actualConflictingFile) return // TODO: rewrite local decls checker to the same logic and then remove the check
            if (areCompatibleMainFunctions(declaration, containingFile, conflicting, actualConflictingFile)) return
            if (isExpectAndActual(declaration, conflicting)) return
            if (
                conflicting is FirMemberDeclaration &&
                !session.visibilityChecker.isVisible(conflicting, session, containingFile, emptyList(), null)
            ) return
            declarationConflictingSymbols.getOrPut(declaration) { SmartSet.create() }.add(conflictingSymbol)
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
                            symbol.ensureResolved(FirResolvePhase.STATUS)
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
    }

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val inspector = DeclarationInspector()

        when (declaration) {
            is FirFile -> checkFile(declaration, inspector, context)
            is FirRegularClass -> checkRegularClass(declaration, inspector)
            else -> {
            }
        }

        context.addDeclaration(declaration)
        try {
            inspector.declarationConflictingSymbols.forEach { (conflictingDeclaration, symbols) ->
                val source = conflictingDeclaration.source
                if (source != null && symbols.isNotEmpty()) {
                    when (conflictingDeclaration) {
                        is FirSimpleFunction,
                        is FirConstructor -> {
                            reporter.reportOn(source, FirErrors.CONFLICTING_OVERLOADS, symbols, context)
                        }
                        else -> {
                            val factory = if (conflictingDeclaration is FirClassLikeDeclaration &&
                                conflictingDeclaration.getContainingDeclaration(context.session) == null &&
                                symbols.any { it is FirClassLikeSymbol<*> }
                            ) {
                                FirErrors.PACKAGE_OR_CLASSIFIER_REDECLARATION
                            } else {
                                FirErrors.REDECLARATION
                            }
                            reporter.reportOn(source, factory, symbols, context)
                        }
                    }
                }
            }

            if (declaration.source?.kind !is KtFakeSourceElementKind) {
                when (declaration) {
                    is FirMemberDeclaration -> {
                        if (declaration is FirFunction) {
                            checkConflictingParameters(declaration.valueParameters, context, reporter)
                        }
                        checkConflictingParameters(declaration.typeParameters, context, reporter)
                    }
                    is FirTypeParametersOwner -> {
                        checkConflictingParameters(declaration.typeParameters, context, reporter)
                    }
                    else -> {
                    }
                }
            }
        } finally {
            context.dropDeclaration()
        }
    }

    private fun checkConflictingParameters(parameters: List<FirElement>, context: CheckerContext, reporter: DiagnosticReporter) {
        if (parameters.size <= 1) return

        val multimap = ListMultimap<Name, FirBasedSymbol<*>>()
        for (parameter in parameters) {
            val name: Name
            val symbol: FirBasedSymbol<*>
            when (parameter) {
                is FirValueParameter -> {
                    symbol = parameter.symbol
                    name = parameter.name
                }
                is FirOuterClassTypeParameterRef -> {
                    continue
                }
                is FirTypeParameterRef -> {
                    symbol = parameter.symbol
                    name = symbol.name
                }
                is FirTypeParameter -> {
                    symbol = parameter.symbol
                    name = parameter.name
                }
                else -> throw AssertionError("Invalid parameter type")
            }
            if (!name.isSpecial) {
                multimap.put(name, symbol)
            }
        }
        for (key in multimap.keys) {
            val conflictingParameters = multimap[key]
            if (conflictingParameters.size > 1) {
                for (parameter in conflictingParameters) {
                    reporter.reportOn(
                        parameter.source,
                        FirErrors.REDECLARATION,
                        conflictingParameters,
                        context
                    )
                }
            }
        }
    }

    private fun checkFile(file: FirFile, inspector: DeclarationInspector, context: CheckerContext) {
        val packageMemberScope: FirPackageMemberScope = context.sessionHolder.scopeSession.getOrBuild(file.packageFqName, PACKAGE_MEMBER) {
            FirPackageMemberScope(file.packageFqName, context.sessionHolder.session)
        }
        for (topLevelDeclaration in file.declarations) {
            inspector.collectWithExternalConflicts(topLevelDeclaration, file, context.session, packageMemberScope)
        }
    }

    private fun checkRegularClass(declaration: FirRegularClass, inspector: DeclarationInspector) {
        for (it in declaration.declarations) {
            inspector.collect(it)
        }
    }
}

private fun FirDeclarationPresenter.represent(declaration: FirDeclaration): String? =
    when (declaration) {
        is FirSimpleFunction -> represent(declaration)
        is FirRegularClass -> represent(declaration)
        is FirTypeAlias -> represent(declaration)
        is FirProperty -> represent(declaration)
        else -> null
    }

class FirNameConflictsTracker : FirNameConflictsTrackerComponent() {

    data class ClassifierWithFile(
        val classifier: FirClassLikeSymbol<*>,
        val file: FirFile?
    )

    val redeclaredClassifiers = HashMap<ClassId, Set<ClassifierWithFile>>()

    override fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: FirClassLikeSymbol<*>, newSymbolFile: FirFile,
        prevSymbol: FirClassLikeSymbol<*>, prevSymbolFile: FirFile?
    ) {
        redeclaredClassifiers.merge(
            classId, linkedSetOf(ClassifierWithFile(newSymbol, newSymbolFile), ClassifierWithFile(prevSymbol, prevSymbolFile))
        ) { a, b -> a + b }
    }
}

private fun FirDeclaration.onConstructors(action: (ctor: FirConstructor) -> Unit) {

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

    acceptChildren(ClassConstructorVisitor())
}


