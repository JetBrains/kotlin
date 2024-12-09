/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirNameConflictsTrackerComponent
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.getDestructuredParameter
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.PACKAGE_MEMBER
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.SmartSet

interface PlatformConflictDeclarationsDiagnosticDispatcher : FirSessionComponent {
    fun getDiagnostic(
        conflictingDeclaration: FirBasedSymbol<*>,
        symbols: SmartSet<FirBasedSymbol<*>>,
        context: CheckerContext
    ): KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>>?

    object DEFAULT : PlatformConflictDeclarationsDiagnosticDispatcher {
        override fun getDiagnostic(
            conflictingDeclaration: FirBasedSymbol<*>,
            symbols: SmartSet<FirBasedSymbol<*>>,
            context: CheckerContext
        ): KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> {
            return when {
                conflictingDeclaration is FirNamedFunctionSymbol || conflictingDeclaration is FirConstructorSymbol -> {
                    FirErrors.CONFLICTING_OVERLOADS
                }
                conflictingDeclaration is FirClassLikeSymbol<*> &&
                        conflictingDeclaration.getContainingClassSymbol() == null &&
                        symbols.any { it is FirClassLikeSymbol<*> } -> {
                    FirErrors.CLASSIFIER_REDECLARATION
                }
                else -> {
                    FirErrors.REDECLARATION
                }
            }
        }
    }
}

val FirSession.conflictDeclarationsDiagnosticDispatcher: PlatformConflictDeclarationsDiagnosticDispatcher? by FirSession.nullableSessionComponentAccessor()

object FirConflictsDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        when (declaration) {
            is FirFile -> {
                val inspector = FirDeclarationCollector<FirBasedSymbol<*>>(context)
                checkFile(declaration, inspector, context)
                reportConflicts(reporter, context, inspector.declarationConflictingSymbols, declaration)
            }
            is FirClass -> {
                if (declaration.source?.kind !is KtFakeSourceElementKind) {
                    checkForLocalRedeclarations(declaration.typeParameters, context, reporter)
                }
                val inspector = FirDeclarationCollector<FirBasedSymbol<*>>(context)
                inspector.collectClassMembers(declaration.symbol)
                reportConflicts(reporter, context, inspector.declarationConflictingSymbols, declaration)
            }
            else -> {
                if (declaration.source?.kind !is KtFakeSourceElementKind && declaration is FirTypeParameterRefsOwner) {
                    if (declaration is FirFunction || declaration is FirProperty) {
                        val destructuredParameters = getDestructuredParameters(declaration)
                        checkForLocalRedeclarations(destructuredParameters, context, reporter)
                    }
                    checkForLocalRedeclarations(declaration.typeParameters, context, reporter)
                }
            }
        }
    }

    private fun getDestructuredParameters(declaration: FirCallableDeclaration): List<FirVariable> {
        return buildList {
            declaration.contextParameters.filterTo(this) { it.valueParameterKind == FirValueParameterKind.ContextParameter }

            if (declaration is FirFunction) {
                addAll(declaration.valueParameters)

                val destructuredParametersBoxes = declaration.valueParameters
                    .filter { it.name == SpecialNames.DESTRUCT }
                    .mapTo(mutableSetOf()) { it.symbol }

                declaration.body?.statements?.mapNotNullTo(this) {
                    (it as? FirVariable)?.takeIf { it.getDestructuredParameter() in destructuredParametersBoxes }
                }
            }

            if (declaration is FirProperty) {
                declaration.setter?.takeUnless { it.source?.kind == KtFakeSourceElementKind.DefaultAccessor }?.valueParameters?.let { addAll(it) }
            }
        }
    }

    private fun reportConflicts(
        reporter: DiagnosticReporter,
        context: CheckerContext,
        declarationConflictingSymbols: Map<FirBasedSymbol<*>, SmartSet<FirBasedSymbol<*>>>,
        container: FirDeclaration,
    ) {
        declarationConflictingSymbols.forEach { (conflictingDeclaration, symbols) ->
            val typeAliasForConstructorSource =
                (conflictingDeclaration as? FirConstructorSymbol)?.typeAliasConstructorInfo?.typeAliasSymbol?.source
            val origin = conflictingDeclaration.origin
            val source = when {
                conflictingDeclaration !is FirCallableSymbol<*> -> conflictingDeclaration.source
                origin == FirDeclarationOrigin.Source -> conflictingDeclaration.source
                origin == FirDeclarationOrigin.Library -> return@forEach
                origin == FirDeclarationOrigin.Synthetic.TypeAliasConstructor -> typeAliasForConstructorSource
                else -> container.source
            }
            if (
                symbols.isEmpty() ||
                // For every primary constructor there is a parent,
                // FirRegularClass declaration, and those clash too,
                // resulting in REDECLARATION.
                conflictingDeclaration.isPrimaryConstructor && symbols.all { it.isPrimaryConstructor }
            ) return@forEach

            if (symbols.singleOrNull()?.let { isExpectAndNonExpect(conflictingDeclaration, it) } == true) {
                reporter.reportOn(source, FirErrors.EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE, conflictingDeclaration, context)
                return@forEach
            }

            val dispatcher = context.session.conflictDeclarationsDiagnosticDispatcher ?: PlatformConflictDeclarationsDiagnosticDispatcher.DEFAULT
            val factory = dispatcher.getDiagnostic(conflictingDeclaration, symbols, context)

            if (factory != null) {
                reporter.reportOn(source, factory, symbols, context)
            }
        }
    }

    private val FirBasedSymbol<*>.isPrimaryConstructor: Boolean
        get() = this is FirConstructorSymbol && isPrimary || origin == FirDeclarationOrigin.Synthetic.TypeAliasConstructor

    private fun checkFile(file: FirFile, inspector: FirDeclarationCollector<FirBasedSymbol<*>>, context: CheckerContext) {
        val packageMemberScope: FirPackageMemberScope =
            context.sessionHolder.scopeSession.getOrBuild(file.packageFqName to context.session, PACKAGE_MEMBER) {
                FirPackageMemberScope(file.packageFqName, context.sessionHolder.session)
            }
        inspector.collectTopLevel(file, packageMemberScope)
    }
}

class FirNameConflictsTracker : FirNameConflictsTrackerComponent() {

    data class ClassifierWithFile(
        val classifier: FirClassLikeSymbol<*>,
        val file: FirFile?,
    )

    private val _redeclaredClassifiers: MutableMap<ClassId, Set<ClassifierWithFile>> = HashMap()
    val redeclaredClassifiers: Map<ClassId, Set<ClassifierWithFile>>
        get() = _redeclaredClassifiers

    override fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: FirClassLikeSymbol<*>, newSymbolFile: FirFile,
        prevSymbol: FirClassLikeSymbol<*>, prevSymbolFile: FirFile?,
    ) {
        _redeclaredClassifiers.merge(
            classId, linkedSetOf(ClassifierWithFile(newSymbol, newSymbolFile), ClassifierWithFile(prevSymbol, prevSymbolFile))
        ) { a, b -> a + b }
    }
}


