/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.call

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.findClosestClassOrObject
import org.jetbrains.kotlin.fir.analysis.checkers.followAllAlias
import org.jetbrains.kotlin.fir.analysis.checkers.isSupertypeOf
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.transformers.firClassLike
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirQualifiedSupertypeExtendedByOtherSupertypeChecker : FirQualifiedAccessChecker() {
    override fun check(functionCall: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // require to be called over a super reference
        val superReference = functionCall.calleeReference.safeAs<FirSuperReference>()
            ?: return

        val explicitType = superReference.superTypeRef.safeAs<FirResolvedTypeRef>()
            ?.firClassLike(context.session)
            ?.followAllAlias(context.session).safeAs<FirClass<*>>()
            ?: return

        val surroundingType = context.findClosestClassOrObject()
            ?: return

        // how many supertypes of `surroundingType`
        // have `explicitType` as their supertype or
        // equal to it
        var count = 0

        for (it in surroundingType.superTypeRefs) {
            val that = it.firClassLike(context.session)
                ?.followAllAlias(context.session).safeAs<FirClass<*>>()
                ?: continue

            if (explicitType == that || explicitType.isSupertypeOf(that)) {
                count += 1

                if (count >= 2) {
                    break
                }
            }
        }

        if (count >= 2) {
            reporter.report(superReference.superTypeRef.source)
        }
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let {
            report(FirErrors.QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE.on(it))
        }
    }
}