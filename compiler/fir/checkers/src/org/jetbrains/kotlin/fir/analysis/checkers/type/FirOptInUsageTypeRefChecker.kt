/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.loadExperimentalities
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.loadExperimentalitiesFromSupertype
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object FirOptInUsageTypeRefChecker : FirTypeRefChecker() {
    @OptIn(SymbolInternals::class)
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = typeRef.source
        if (source?.kind !is KtRealSourceElementKind) return
        val coneType = typeRef.coneTypeSafe<ConeClassLikeType>() ?: return
        val symbol = typeRef.findSymbol(context.session) ?: return

        val typeAliasExpandedSymbol = (symbol as? FirTypeAliasSymbol)?.resolvedExpandedTypeRef?.findSymbol(context.session)
        val processedSymbol = typeAliasExpandedSymbol ?: symbol
        val classId = processedSymbol.classId
        val lastAnnotationCall = context.callsOrAssignments.lastOrNull() as? FirAnnotation
        if (lastAnnotationCall == null || lastAnnotationCall.annotationTypeRef !== typeRef) {
            if (classId == OptInNames.REQUIRES_OPT_IN_CLASS_ID || classId == OptInNames.OPT_IN_CLASS_ID) {
                reporter.reportOn(source, OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION, context)
            } else if (processedSymbol is FirRegularClassSymbol &&
                processedSymbol.fir.getAnnotationByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID, context.session) != null
            ) {
                reporter.reportOn(source, OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN, context)
            }
        }

        val isSupertypeRef = typeRef in (context.containingDeclarations.lastOrNull() as? FirClass)?.superTypeRefs.orEmpty()
        with(FirOptInUsageBaseChecker) {
            val experimentalities = mutableSetOf<FirOptInUsageBaseChecker.Experimentality>()
            experimentalities.addAll(symbol.loadClassifierExperimentalities(context, isSupertypeRef))
            if (typeAliasExpandedSymbol != null) experimentalities.addAll(typeAliasExpandedSymbol.loadClassifierExperimentalities(context, isSupertypeRef))
            experimentalities.addAll(loadExperimentalitiesFromConeArguments(context, coneType.typeArguments.toList()))
            reportNotAcceptedExperimentalities(experimentalities, typeRef, context, reporter)
        }
    }

    private fun FirClassLikeSymbol<*>.loadClassifierExperimentalities(context: CheckerContext, isSupertypeRef: Boolean) =
        if (isSupertypeRef) loadExperimentalitiesFromSupertype(context) else loadExperimentalities(
            context,
            fromSetter = false,
            dispatchReceiverType = null
        )

    private fun FirTypeRef.findSymbol(session: FirSession): FirClassLikeSymbol<*>? {
        // coneTypeSafe filters out all delegatedTypeRefs from here
        val coneType = coneTypeSafe<ConeClassLikeType>() ?: return null
        return coneType.lookupTag.toSymbol(session)
    }
}
