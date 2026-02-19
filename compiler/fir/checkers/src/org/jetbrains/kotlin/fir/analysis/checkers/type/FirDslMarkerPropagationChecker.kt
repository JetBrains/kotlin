/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.StandardClassIds

object FirDslMarkerPropagationChecker : FirFunctionTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirFunctionTypeRef) {
        val implicitValueSize = typeRef.contextParameterTypeRefs.size + (if (typeRef.receiverTypeRef != null) 1 else 0)
        if (implicitValueSize <= 1) return
        for (annotation in typeRef.annotations.filter { it.isDslMarker(context.session) }) {
            reporter.reportOn(annotation.source, FirErrors.DSL_MARKER_PROPAGATES_TO_MANY)
        }
    }

    fun FirAnnotation.isDslMarker(session: FirSession): Boolean =
        annotationTypeRef.coneType
            .fullyExpandedType(session).toClassSymbol(session)
            ?.hasAnnotation(StandardClassIds.Annotations.DslMarker, session) == true
}