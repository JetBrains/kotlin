/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.argumentMapping
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds

object FirAnnotationChecker : FirAnnotatedDeclarationChecker() {
    private val deprecatedClassId = FqName("kotlin.Deprecated")
    private val deprecatedSinceKotlinClassId = FqName("kotlin.DeprecatedSinceKotlin")

    override fun check(
        declaration: FirAnnotatedDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        var deprecatedCall: FirAnnotationCall? = null
        var deprecatedSinceKotlinCall: FirAnnotationCall? = null

        for (annotation in declaration.annotations) {
            val fqName = annotation.fqName(context.session)
            if (fqName == deprecatedClassId) {
                deprecatedCall = annotation
            } else if (fqName == deprecatedSinceKotlinClassId) {
                deprecatedSinceKotlinCall = annotation
            }
        }

        if (deprecatedSinceKotlinCall != null) {
            val closestFirFile = context.findClosest<FirFile>()
            if (closestFirFile != null && !closestFirFile.packageFqName.startsWith(StandardClassIds.BASE_KOTLIN_PACKAGE.shortName())) {
                reporter.reportOn(
                    deprecatedSinceKotlinCall.source,
                    FirErrors.DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE,
                    context
                )
            }

            if (deprecatedCall == null) {
                reporter.reportOn(deprecatedSinceKotlinCall.source, FirErrors.DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED, context)
            } else {
                val argumentMapping = deprecatedCall.argumentMapping ?: return
                for (value in argumentMapping.values) {
                    if (value.name.identifier == "level") {
                        reporter.reportOn(
                            deprecatedSinceKotlinCall.source,
                            FirErrors.DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL,
                            context
                        )
                        break
                    }
                }
            }
        }
    }
}