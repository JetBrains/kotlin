/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.isExperimentalMarker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.loadExperimentalities
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.loadExperimentalitiesFromSupertype
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.getContainingClassLookupTag
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object FirOptInUsageTypeRefChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    override fun check(typeRef: FirResolvedTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = typeRef.source
        val delegatedTypeRef = typeRef.delegatedTypeRef
        if (source?.kind !is KtRealSourceElementKind) return
        // ConeClassLikeType filters out all delegatedTypeRefs from here
        val expandedTypealiasType = typeRef.coneType.fullyExpandedType(context.session).lowerBoundIfFlexible() as? ConeClassLikeType ?: return
        val coneType = expandedTypealiasType.abbreviatedTypeOrSelf as? ConeClassLikeType ?: return
        val symbol = coneType.toSymbol(context.session) ?: return

        val typeAliasExpandedSymbol = expandedTypealiasType.takeIf { it.isTypealiasExpansion }?.toSymbol(context.session)
        val processedSymbol = typeAliasExpandedSymbol ?: symbol

        val classId = processedSymbol.classId
        val lastAnnotationCall = context.callsOrAssignments.lastOrNull() as? FirAnnotation
        if (lastAnnotationCall == null || lastAnnotationCall.annotationTypeRef !== typeRef) {
            when {
                classId == OptInNames.REQUIRES_OPT_IN_CLASS_ID || classId == OptInNames.OPT_IN_CLASS_ID ->
                    reporter.reportOn(source, OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION, context)
                processedSymbol.isExperimentalMarker(context.session) ->
                    reporter.reportOn(source, OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN, context)
                delegatedTypeRef is FirUserTypeRef && delegatedTypeRef.qualifier.isNotEmpty() -> {
                    processedSymbol.checkContainingClasses(source, delegatedTypeRef.qualifier, context, reporter)
                }
            }
        }
        val isSupertypeRef = typeRef in (context.containingDeclarations.lastOrNull() as? FirClass)?.superTypeRefs.orEmpty()
        with(FirOptInUsageBaseChecker) {
            val experimentalities = mutableSetOf<FirOptInUsageBaseChecker.Experimentality>()
            experimentalities.addAll(symbol.loadClassifierExperimentalities(context, isSupertypeRef))
            if (typeAliasExpandedSymbol != null) {
                experimentalities.addAll(
                    typeAliasExpandedSymbol.loadClassifierExperimentalities(
                        context,
                        isSupertypeRef
                    )
                )
            }
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

    private tailrec fun FirClassLikeSymbol<*>.checkContainingClasses(
        source: KtSourceElement,
        qualifier: List<FirQualifierPart>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val containingClassSymbol = this.getContainingClassLookupTag()?.toSymbol(context.session) ?: return
        if (qualifier.any { it.name == containingClassSymbol.name } && containingClassSymbol.isExperimentalMarker(context.session)) {
            reporter.reportOn(source, OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN, context)
        }
        containingClassSymbol.checkContainingClasses(source, qualifier, context, reporter)
    }
}
