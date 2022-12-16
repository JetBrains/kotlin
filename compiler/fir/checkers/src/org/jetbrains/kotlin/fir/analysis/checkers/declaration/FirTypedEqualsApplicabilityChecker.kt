/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.fir.types.isNothing
import org.jetbrains.kotlin.fir.types.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions


object FirTypedEqualsApplicabilityChecker : FirSimpleFunctionChecker() {

    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation = declaration.getAnnotationByClassId(StandardClassIds.Annotations.TypedEquals) ?: return
        if (!declaration.hasSuitableSignatureForTypedEquals(context.session)) {
            reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TYPED_EQUALS_ANNOTATION, context)
        }
    }

    private fun FirSimpleFunction.hasSuitableSignatureForTypedEquals(session: FirSession): Boolean =
        containingClassLookupTag()?.toFirRegularClassSymbol(session)?.run {
            val valueClassStarProjection = this@run.defaultType().replaceArgumentsWithStarProjections()
            with(this@hasSuitableSignatureForTypedEquals) {
                contextReceivers.isEmpty() && receiverParameter == null
                        && name == OperatorNameConventions.EQUALS
                        && this@run.isInline && valueParameters.size == 1
                        && (returnTypeRef.isBoolean || returnTypeRef.isNothing)
                        && valueParameters[0].returnTypeRef.coneType == valueClassStarProjection
            }
        } ?: false
}