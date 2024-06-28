/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.native.checkers.FirNativeObjCRefinementChecker.hidesFromObjCClassId
import org.jetbrains.kotlin.fir.analysis.native.checkers.FirNativeObjCRefinementChecker.refinesInSwiftClassId
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassLikeSymbol
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.isIntersectionOverride
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenMembersWithBaseScope
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

sealed class FirNativeObjCRefinementOverridesChecker(mppKind: MppCheckerKind) : FirClassChecker(mppKind) {
    object Regular : FirNativeObjCRefinementOverridesChecker(MppCheckerKind.Platform) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    object ForExpectClass : FirNativeObjCRefinementOverridesChecker(MppCheckerKind.Common) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (!declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        // We just need to check intersection overrides, all other declarations are checked by FirNativeObjCRefinementChecker
        val baseScope = declaration.unsubstitutedScope(context)
        baseScope.processAllFunctions { symbol ->
            if (!symbol.isIntersectionOverride) return@processAllFunctions
            check(baseScope, symbol, declaration, context, reporter, emptyList(), emptyList())
        }
        baseScope.processAllProperties { symbol ->
            if (!symbol.isIntersectionOverride) return@processAllProperties
            check(baseScope, symbol, declaration, context, reporter, emptyList(), emptyList())
        }
    }

    companion object {
        fun check(
            baseScope: FirTypeScope,
            memberSymbol: FirCallableSymbol<*>,
            declarationToReport: FirDeclaration,
            context: CheckerContext,
            reporter: DiagnosticReporter,
            objCAnnotations: List<FirAnnotation>,
            swiftAnnotations: List<FirAnnotation>
        ) {
            val overriddenMemberSymbols = baseScope.getDirectOverriddenMembersWithBaseScope(memberSymbol)
            if (overriddenMemberSymbols.isEmpty()) return
            var isHiddenFromObjC = objCAnnotations.isNotEmpty()
            var isRefinedInSwift = swiftAnnotations.isNotEmpty()
            val supersNotHiddenFromObjC = mutableListOf<FirCallableSymbol<*>>()
            val supersNotRefinedInSwift = mutableListOf<FirCallableSymbol<*>>()
            for ((symbol, scope) in overriddenMemberSymbols) {
                val (superIsHiddenFromObjC, superIsRefinedInSwift) = symbol.inheritsRefinedAnnotations(context.session, scope)
                if (superIsHiddenFromObjC) isHiddenFromObjC = true else supersNotHiddenFromObjC.add(symbol)
                if (superIsRefinedInSwift) isRefinedInSwift = true else supersNotRefinedInSwift.add(symbol)
            }
            if (isHiddenFromObjC && supersNotHiddenFromObjC.isNotEmpty()) {
                reporter.reportIncompatibleOverride(declarationToReport, objCAnnotations, supersNotHiddenFromObjC, context)
            }
            if (isRefinedInSwift && supersNotRefinedInSwift.isNotEmpty()) {
                reporter.reportIncompatibleOverride(declarationToReport, swiftAnnotations, supersNotRefinedInSwift, context)
            }
        }

        private fun FirCallableSymbol<*>.inheritsRefinedAnnotations(session: FirSession, baseScope: FirTypeScope): Pair<Boolean, Boolean> {
            val (hasObjC, hasSwift) = hasRefinedAnnotations(session)
            if (hasObjC && hasSwift) return true to true
            // Note: `checkMember` requires all overridden symbols to be either refined or not refined.
            val (overriddenMemberSymbol, scope) = baseScope.getDirectOverriddenMembersWithBaseScope(this).firstOrNull()
                ?: return hasObjC to hasSwift
            val (inheritsObjC, inheritsSwift) = overriddenMemberSymbol.inheritsRefinedAnnotations(session, scope)
            return (hasObjC || inheritsObjC) to (hasSwift || inheritsSwift)
        }

        private fun FirCallableSymbol<*>.hasRefinedAnnotations(session: FirSession): Pair<Boolean, Boolean> {
            var hasObjC = false
            var hasSwift = false
            for (annotation in resolvedAnnotationsWithClassIds) {
                val metaAnnotations = annotation.toAnnotationClassLikeSymbol(session)?.resolvedAnnotationsWithClassIds.orEmpty()
                for (metaAnnotation in metaAnnotations) {
                    when (metaAnnotation.toAnnotationClassId(session)) {
                        hidesFromObjCClassId -> {
                            hasObjC = true
                            break
                        }

                        refinesInSwiftClassId -> {
                            hasSwift = true
                            break
                        }
                    }
                }
                if (hasObjC && hasSwift) return true to true
            }
            return hasObjC to hasSwift
        }

        private fun DiagnosticReporter.reportIncompatibleOverride(
            declaration: FirDeclaration,
            annotations: List<FirAnnotation>,
            notRefinedSupers: List<FirCallableSymbol<*>>,
            context: CheckerContext
        ) {
            val containingDeclarations = notRefinedSupers.mapNotNull { it.containingClassLookupTag()?.toFirRegularClassSymbol(context.session) }
            if (annotations.isEmpty()) {
                reportOn(declaration.source, INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE, declaration.symbol, containingDeclarations, context)
            } else {
                for (annotation in annotations) {
                    reportOn(annotation.source, INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE, declaration.symbol, containingDeclarations, context)
                }
            }
        }

    }
}
