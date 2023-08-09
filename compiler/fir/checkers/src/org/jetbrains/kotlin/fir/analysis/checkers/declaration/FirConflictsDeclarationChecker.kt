/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirNameConflictsTrackerComponent
import org.jetbrains.kotlin.fir.analysis.checkers.FirDeclarationCollector
import org.jetbrains.kotlin.fir.analysis.checkers.checkForLocalRedeclarations
import org.jetbrains.kotlin.fir.analysis.checkers.collectClassMembers
import org.jetbrains.kotlin.fir.analysis.checkers.collectTopLevel
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.PACKAGE_MEMBER
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.SmartSet

object FirConflictsDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        when (declaration) {
            is FirFile -> {
                val inspector = FirDeclarationCollector<FirDeclaration>(context)
                checkFile(declaration, inspector, context)
                reportConflicts(reporter, context, inspector.declarationConflictingSymbols)
            }
            is FirRegularClass -> {
                if (declaration.source?.kind !is KtFakeSourceElementKind) {
                    checkForLocalRedeclarations(declaration.typeParameters, context, reporter)
                }
                val inspector = FirDeclarationCollector<FirDeclaration>(context)
                inspector.collectClassMembers(declaration)
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
        declarationConflictingSymbols: Map<FirDeclaration, SmartSet<FirBasedSymbol<*>>>,
    ) {
        declarationConflictingSymbols.forEach { (conflictingDeclaration, symbols) ->
            val source = conflictingDeclaration.source
            if (symbols.isEmpty()) return@forEach

            val factory =
                if (conflictingDeclaration is FirSimpleFunction || conflictingDeclaration is FirConstructor) {
                    FirErrors.CONFLICTING_OVERLOADS
                } else if (conflictingDeclaration is FirClassLikeDeclaration &&
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

    private fun checkFile(file: FirFile, inspector: FirDeclarationCollector<FirDeclaration>, context: CheckerContext) {
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


