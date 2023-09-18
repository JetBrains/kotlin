/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirNameConflictsTrackerComponent
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.PACKAGE_MEMBER
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasForConstructor
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.SmartSet

object FirConflictsDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        when (declaration) {
            is FirFile -> {
                val inspector = FirDeclarationCollector<FirBasedSymbol<*>>(context)
                checkFile(declaration, inspector, context)
                reportConflicts(reporter, context, inspector.declarationConflictingSymbols)
            }
            is FirRegularClass -> {
                if (declaration.source?.kind !is KtFakeSourceElementKind) {
                    checkForLocalRedeclarations(declaration.typeParameters, context, reporter)
                }
                val inspector = FirDeclarationCollector<FirBasedSymbol<*>>(context)
                inspector.collectClassMembers(declaration.symbol)
                reportConflicts(reporter, context, inspector.declarationConflictingSymbols)
            }
            else -> {
                if (declaration.source?.kind !is KtFakeSourceElementKind && declaration is FirTypeParameterRefsOwner) {
                    if (declaration is FirFunction) {
                        checkForLocalRedeclarations(declaration.valueParameters, context, reporter)
                    }
                    checkForLocalRedeclarations(declaration.typeParameters, context, reporter)
                }
            }
        }
    }

    private fun reportConflicts(
        reporter: DiagnosticReporter,
        context: CheckerContext,
        declarationConflictingSymbols: Map<FirBasedSymbol<*>, SmartSet<FirBasedSymbol<*>>>,
    ) {
        declarationConflictingSymbols.forEach { (conflictingDeclaration, symbols) ->
            val typeAliasForConstructorSource = (conflictingDeclaration as? FirConstructorSymbol)?.typeAliasForConstructor?.source
            val source = typeAliasForConstructorSource ?: conflictingDeclaration.source
            if (
                symbols.isEmpty() ||
                // For every implicit constructor there is a parent,
                // FirRegularClass declaration, and those clash too,
                // resulting in REDECLARATION.
                conflictingDeclaration.isImplicitConstructor && symbols.all { it.isImplicitConstructor }
            ) return@forEach

            val factory =
                if (conflictingDeclaration is FirNamedFunctionSymbol || conflictingDeclaration is FirConstructorSymbol) {
                    FirErrors.CONFLICTING_OVERLOADS
                } else if (conflictingDeclaration is FirClassLikeSymbol<*> &&
                    conflictingDeclaration.getContainingClassSymbol(context.session) == null &&
                    symbols.any { it is FirClassLikeSymbol<*> }
                ) {
                    FirErrors.PACKAGE_OR_CLASSIFIER_REDECLARATION
                } else {
                    FirErrors.REDECLARATION
                }

            reporter.reportOn(source, factory, symbols, context)
        }
    }

    private val FirBasedSymbol<*>.isImplicitConstructor get() = source?.kind is KtFakeSourceElementKind.ImplicitConstructor

    private fun checkFile(file: FirFile, inspector: FirDeclarationCollector<FirBasedSymbol<*>>, context: CheckerContext) {
        val packageMemberScope: FirPackageMemberScope = context.sessionHolder.scopeSession.getOrBuild(file.packageFqName, PACKAGE_MEMBER) {
            FirPackageMemberScope(file.packageFqName, context.sessionHolder.session)
        }
        inspector.collectTopLevel(file, packageMemberScope)
        for (topLevelDeclaration in file.declarations) {
            if (topLevelDeclaration is FirErrorProperty || topLevelDeclaration is FirErrorFunction) continue

        }
    }
}

class FirNameConflictsTracker : FirNameConflictsTrackerComponent() {

    data class ClassifierWithFile(
        val classifier: FirClassLikeSymbol<*>,
        val file: FirFile?,
    )

    val redeclaredClassifiers = HashMap<ClassId, Set<ClassifierWithFile>>()

    override fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: FirClassLikeSymbol<*>, newSymbolFile: FirFile,
        prevSymbol: FirClassLikeSymbol<*>, prevSymbolFile: FirFile?,
    ) {
        redeclaredClassifiers.merge(
            classId, linkedSetOf(ClassifierWithFile(newSymbol, newSymbolFile), ClassifierWithFile(prevSymbol, prevSymbolFile))
        ) { a, b -> a + b }
    }
}


