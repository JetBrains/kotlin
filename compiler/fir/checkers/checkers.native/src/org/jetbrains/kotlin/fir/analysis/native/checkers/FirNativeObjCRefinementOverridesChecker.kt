/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
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
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.unexpandedConeClassLikeType
import org.jetbrains.kotlin.fir.isIntersectionOverride
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

object FirNativeObjCRefinementOverridesChecker : FirClassChecker() {

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        // We just need to check intersection overrides, all other declarations are checked by FirNativeObjCRefinementChecker
        val firTypeScope = declaration.unsubstitutedScope(context)
        firTypeScope.processAllFunctions { symbol ->
            if (!symbol.isIntersectionOverride) return@processAllFunctions
            check(firTypeScope, symbol, declaration, context, reporter, emptyList(), emptyList())
        }
        firTypeScope.processAllProperties { symbol ->
            if (!symbol.isIntersectionOverride) return@processAllProperties
            check(firTypeScope, symbol, declaration, context, reporter, emptyList(), emptyList())
        }
    }

    fun check(
        firTypeScope: FirTypeScope,
        memberSymbol: FirCallableSymbol<*>,
        declarationToReport: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        objCAnnotations: List<FirAnnotation>,
        swiftAnnotations: List<FirAnnotation>
    ) {
        val overriddenMemberSymbols = firTypeScope.retrieveDirectOverriddenOf(memberSymbol)
        if (overriddenMemberSymbols.isEmpty()) return
        var isHiddenFromObjC = objCAnnotations.isNotEmpty()
        var isRefinedInSwift = swiftAnnotations.isNotEmpty()
        val supersNotHiddenFromObjC = mutableListOf<FirCallableSymbol<*>>()
        val supersNotRefinedInSwift = mutableListOf<FirCallableSymbol<*>>()
        for (symbol in overriddenMemberSymbols) {
            val (superIsHiddenFromObjC, superIsRefinedInSwift) = symbol.inheritsRefinedAnnotations(context.session, firTypeScope)
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

    private fun FirCallableSymbol<*>.inheritsRefinedAnnotations(session: FirSession, firTypeScope: FirTypeScope): Pair<Boolean, Boolean> {
        val (hasObjC, hasSwift) = hasRefinedAnnotations(session)
        if (hasObjC && hasSwift) return true to true
        // Note: `checkMember` requires all overridden symbols to be either refined or not refined.
        val overriddenMemberSymbol = firTypeScope.retrieveDirectOverriddenOf(this).firstOrNull()
            ?: return hasObjC to hasSwift
        val (inheritsObjC, inheritsSwift) = overriddenMemberSymbol.inheritsRefinedAnnotations(session, firTypeScope)
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
