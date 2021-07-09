/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationByClassId
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object FirOptInUsageTypeRefChecker : FirTypeRefChecker() {
    @OptIn(SymbolInternals::class)
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = typeRef.source
        if (source?.kind !is FirRealSourceElementKind) return
        val coneType = typeRef.coneTypeSafe<ConeClassLikeType>() ?: return
        val symbol = coneType.lookupTag.toSymbol(context.session) ?: return
        symbol.ensureResolved(FirResolvePhase.STATUS)
        val classId = symbol.classId
        val lastAnnotationCall = context.qualifiedAccessOrAnnotationCalls.lastOrNull() as? FirAnnotationCall
        if (lastAnnotationCall == null || lastAnnotationCall.annotationTypeRef !== typeRef) {
            if (classId == OptInNames.REQUIRES_OPT_IN_CLASS_ID || classId == OptInNames.OPT_IN_CLASS_ID) {
                reporter.reportOn(source, EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION, context)
            } else if (symbol is FirRegularClassSymbol && symbol.fir.getAnnotationByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID) != null) {
                reporter.reportOn(source, EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL, context)
            }
        }

        with(FirOptInUsageBaseChecker) {
            val experimentalities = symbol.fir.loadExperimentalities(context, fromSetter = false) +
                    loadExperimentalitiesFromConeArguments(context, coneType.typeArguments.toList())
            reportNotAcceptedExperimentalities(experimentalities, typeRef, context, reporter)
        }
    }
}