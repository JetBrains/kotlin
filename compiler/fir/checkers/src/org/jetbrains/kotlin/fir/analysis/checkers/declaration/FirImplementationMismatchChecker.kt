/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.impl.delegatedWrapperData
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionCallableSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirImplementationMismatchChecker : FirClassChecker() {

    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source ?: return
        val sourceKind = source.kind
        if (sourceKind is FirFakeSourceElementKind && sourceKind != FirFakeSourceElementKind.EnumInitializer) return
        if (declaration is FirRegularClass && declaration.isExpect) return
        val classKind = declaration.classKind
        if (classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.ENUM_CLASS) return

        val typeCheckerContext = context.session.typeContext.newBaseTypeCheckerContext(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )
        val classScope = declaration.unsubstitutedScope(context)
        val dedupReporter = reporter.deduplicating()

        fun checkSymbol(symbol: FirCallableSymbol<*>) {
            if (symbol.callableId.classId != declaration.classId) return
            if (symbol !is FirIntersectionCallableSymbol) return
            val withTypes = symbol.intersections.map {
                it.fir to context.returnTypeCalculator.tryCalculateReturnType(it.fir).coneType
            }

            if (withTypes.any { it.second is ConeKotlinErrorType }) return

            var delegation: FirCallableDeclaration<*>? = null
            val implementations = mutableListOf<FirCallableDeclaration<*>>()

            for (intSymbol in symbol.intersections) {
                val fir = intSymbol.fir
                if (fir.delegatedWrapperData?.containingClass?.classId == declaration.classId) {
                    delegation = fir
                    break
                }
                if (!(fir as FirCallableMemberDeclaration<*>).isAbstract) {
                    implementations.add(fir)
                }
            }

            if (delegation != null || implementations.isNotEmpty()) {
                //if there are more than one implementation we report nothing because it will be reported differently
                val method = delegation ?: implementations.singleOrNull() ?: return
                val methodType = context.returnTypeCalculator.tryCalculateReturnType(method).coneType
                val (conflict, _) = withTypes.find { (_, type) ->
                    !AbstractTypeChecker.isSubtypeOf(typeCheckerContext, methodType, type)
                } ?: return
                val error =
                    if (delegation != null) FirErrors.RETURN_TYPE_MISMATCH_BY_DELEGATION
                    else FirErrors.RETURN_TYPE_MISMATCH_ON_INHERITANCE
                dedupReporter.reportOn(source, error, method, conflict, context)
            } else {
                //if there is no implementation, check that there can be any type compatible (subtype of) with all
                var clash: Pair<FirCallableDeclaration<*>, FirCallableDeclaration<*>>? = null
                val compatible = withTypes.any { (m1, type1) ->
                    withTypes.all { (m2, type2) ->
                        val result = AbstractTypeChecker.isSubtypeOf(typeCheckerContext, type1, type2)
                        if (!result && clash == null && !AbstractTypeChecker.isSubtypeOf(typeCheckerContext, type2, type1)) {
                            clash = m1 to m2
                        }
                        result
                    }
                }
                clash?.takeIf { !compatible }?.let { (m1, m2) ->
                    dedupReporter.reportOn(source, FirErrors.RETURN_TYPE_MISMATCH_ON_INHERITANCE, m1, m2, context)
                }
            }
        }

        for (name in classScope.getCallableNames()) {
            classScope.processFunctionsByName(name, ::checkSymbol)
        }
    }
}