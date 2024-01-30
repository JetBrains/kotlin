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
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.SmartSet

object FirConflictsDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        when (declaration) {
            is FirFile -> {
                val inspector = FirDeclarationCollector<FirBasedSymbol<*>>(context)
                checkFile(declaration, inspector, context)
                reportConflicts(reporter, context, inspector.declarationConflictingSymbols, declaration)
            }
            is FirRegularClass -> {
                if (declaration.source?.kind !is KtFakeSourceElementKind) {
                    checkForLocalRedeclarations(declaration.typeParameters, context, reporter)
                }
                val inspector = FirDeclarationCollector<FirBasedSymbol<*>>(context)
                inspector.collectClassMembers(declaration.symbol)
                reportConflicts(reporter, context, inspector.declarationConflictingSymbols, declaration)
            }
            else -> {
                if (declaration.source?.kind !is KtFakeSourceElementKind && declaration is FirTypeParameterRefsOwner) {
                    if (declaration is FirFunction) {
                        val destructuredParameters = getDestructuredParameters(declaration)
                        checkForLocalRedeclarations(destructuredParameters, context, reporter)
                    }
                    checkForLocalRedeclarations(declaration.typeParameters, context, reporter)
                }
            }
        }
    }

    private fun getDestructuredParameters(function: FirFunction): List<FirVariable> {
        if (function.valueParameters.none { it.name == SpecialNames.DESTRUCT }) return function.valueParameters
        val destructuredParametersBoxes = function.valueParameters
            .filter { it.name == SpecialNames.DESTRUCT }
            .mapTo(mutableSetOf()) { it.symbol }

        return function.body?.statements.orEmpty().mapNotNullTo(function.valueParameters.toMutableList()) {
            val destructuredParameter = (it as? FirVariable)?.getDestructuredParameter() ?: return@mapNotNullTo null
            if (destructuredParameter in destructuredParametersBoxes) it else null
        }
    }

    private fun reportConflicts(
        reporter: DiagnosticReporter,
        context: CheckerContext,
        declarationConflictingSymbols: Map<FirBasedSymbol<*>, SmartSet<FirBasedSymbol<*>>>,
        container: FirDeclaration,
    ) {
        declarationConflictingSymbols.forEach { (conflictingDeclaration, symbols) ->
            val typeAliasForConstructorSource = (conflictingDeclaration as? FirConstructorSymbol)?.typeAliasForConstructor?.source
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

            when {
                symbols.singleOrNull()?.let { isExpectAndActual(conflictingDeclaration, it) } == true -> {
                    reporter.reportOn(source, FirErrors.EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE, conflictingDeclaration, context)
                }
                conflictingDeclaration is FirNamedFunctionSymbol || conflictingDeclaration is FirConstructorSymbol -> {
                    reporter.reportOn(source, FirErrors.CONFLICTING_OVERLOADS, symbols, context)
                }
                conflictingDeclaration is FirClassLikeSymbol<*> &&
                        conflictingDeclaration.getContainingClassSymbol(context.session) == null &&
                        symbols.any { it is FirClassLikeSymbol<*> } -> {
                    reporter.reportOn(source, FirErrors.PACKAGE_OR_CLASSIFIER_REDECLARATION, symbols, context)
                }
                else -> {
                    reporter.reportOn(source, FirErrors.REDECLARATION, symbols, context)
                }
            }
        }
    }

    private val FirBasedSymbol<*>.isPrimaryConstructor: Boolean
        get() = this is FirConstructorSymbol && isPrimary || origin == FirDeclarationOrigin.Synthetic.TypeAliasConstructor

    private fun checkFile(file: FirFile, inspector: FirDeclarationCollector<FirBasedSymbol<*>>, context: CheckerContext) {
        val packageMemberScope: FirPackageMemberScope = context.sessionHolder.scopeSession.getOrBuild(file.packageFqName, PACKAGE_MEMBER) {
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


