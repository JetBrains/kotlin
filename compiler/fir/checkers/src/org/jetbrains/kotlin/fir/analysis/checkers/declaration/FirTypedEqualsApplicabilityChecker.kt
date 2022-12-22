/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.fir.types.isNothing
import org.jetbrains.kotlin.fir.types.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions


object FirTypedEqualsApplicabilityChecker : FirFunctionChecker() {

    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val typedEqualsAnnotation = declaration.getAnnotationByClassId(StandardClassIds.Annotations.TypedEquals) ?: return
        if (declaration !is FirSimpleFunction) {
            reporter.reportOn(
                typedEqualsAnnotation.source,
                FirErrors.INAPPLICABLE_TYPED_EQUALS_ANNOTATION,
                "function must be a member of value class",
                context
            )
            return
        }
        val parentClass =
            declaration.containingClassLookupTag()?.toFirRegularClassSymbol(context.session)?.takeIf { it.isInlineOrValueClass() }
        if (parentClass == null) {
            reporter.reportOn(
                typedEqualsAnnotation.source,
                FirErrors.INAPPLICABLE_TYPED_EQUALS_ANNOTATION,
                "function must be a member of value class",
                context
            )
            return
        }
        if (!parentClass.annotations.hasAnnotation(StandardClassIds.Annotations.AllowTypedEquals)) {
            reporter.reportOn(
                typedEqualsAnnotation.source,
                FirErrors.INAPPLICABLE_TYPED_EQUALS_ANNOTATION,
                "containing class must be annotated by @AllowTypedEquals",
                context
            )
            return
        }
        if (!declaration.hasSuitableSignatureForTypedEquals(parentClass)) {
            reporter.reportOn(typedEqualsAnnotation.source, FirErrors.INAPPLICABLE_TYPED_EQUALS_ANNOTATION, "unexpected signature", context)
        }
    }

    private fun FirSimpleFunction.hasSuitableSignatureForTypedEquals(parentClass: FirRegularClassSymbol): Boolean =
        contextReceivers.isEmpty() && receiverParameter == null
                && name == OperatorNameConventions.EQUALS
                && valueParameters.size == 1
                && (returnTypeRef.isBoolean || returnTypeRef.isNothing)
                && valueParameters[0].returnTypeRef.coneType == parentClass.defaultType().replaceArgumentsWithStarProjections()
}