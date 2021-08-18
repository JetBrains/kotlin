/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.*

object FirJvmPackageNameAnnotationsChecker : FirAnnotationCallChecker() {

    private val jvmPackageNameClassId = ClassId.topLevel(FqName("kotlin.jvm.JvmPackageName"))
    private val stringParameterName = Name.identifier("name")

    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val lookupTag = expression.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag ?: return
        if (lookupTag.classId != jvmPackageNameClassId) return

        val nameValue = expression.getStringArgument(stringParameterName) ?: return
        if (nameValue.isEmpty()) {
            reporter.reportOn(expression.source, FirJvmErrors.JVM_PACKAGE_NAME_CANNOT_BE_EMPTY, context)
        } else if (!isValidJavaFqName(nameValue)) {
            reporter.reportOn(expression.source, FirJvmErrors.JVM_PACKAGE_NAME_MUST_BE_VALID_NAME, context)
        }

        val file = context.containingDeclarations.firstOrNull() as? FirFile ?: return
        if (file.declarations.any { it is FirClass }) {
            reporter.reportOn(expression.source, FirJvmErrors.JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES, context)
        }
    }
}