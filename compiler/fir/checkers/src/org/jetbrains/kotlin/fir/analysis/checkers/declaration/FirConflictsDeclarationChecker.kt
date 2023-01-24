/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirNameConflictsTrackerComponent
import org.jetbrains.kotlin.fir.analysis.checkers.FirDeclarationInspector
import org.jetbrains.kotlin.fir.analysis.checkers.checkConflictingElements
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.PACKAGE_MEMBER
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId

object FirConflictsDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val inspector: FirDeclarationInspector?

        when (declaration) {
            is FirFile -> {
                inspector = FirDeclarationInspector()
                checkFile(declaration, inspector, context)
            }
            is FirRegularClass -> {
                if (declaration.source?.kind !is KtFakeSourceElementKind) {
                    checkConflictingElements(declaration.typeParameters, context, reporter)
                }
                inspector = FirDeclarationInspector()
                checkRegularClass(declaration, inspector)
            }
            else -> {
                if (declaration.source?.kind !is KtFakeSourceElementKind && declaration is FirTypeParameterRefsOwner) {
                    if (declaration is FirFunction) {
                        checkConflictingElements(declaration.valueParameters, context, reporter)
                    }
                    checkConflictingElements(declaration.typeParameters, context, reporter)
                }
                return
            }
        }

        inspector.declarationConflictingSymbols.forEach { (conflictingDeclaration, symbols) ->
            val source = conflictingDeclaration.source
            if (symbols.isNotEmpty()) {
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
    }

    private fun checkFile(file: FirFile, inspector: FirDeclarationInspector, context: CheckerContext) {
        val packageMemberScope: FirPackageMemberScope = context.sessionHolder.scopeSession.getOrBuild(file.packageFqName, PACKAGE_MEMBER) {
            FirPackageMemberScope(file.packageFqName, context.sessionHolder.session)
        }
        for (topLevelDeclaration in file.declarations) {
            inspector.collectWithExternalConflicts(topLevelDeclaration, file, context.session, packageMemberScope)
        }
    }

    private fun checkRegularClass(declaration: FirRegularClass, inspector: FirDeclarationInspector) {
        for (it in declaration.declarations) {
            inspector.collect(it)
        }
    }
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


