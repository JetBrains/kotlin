/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName

object FirJvmPackageNameAnnotationsChecker : FirAnnotationChecker(MppCheckerKind.Common) {

    private val jvmPackageNameClassId = ClassId.topLevel(FqName("kotlin.jvm.JvmPackageName"))

    override fun check(expression: FirAnnotation, context: CheckerContext, reporter: DiagnosticReporter) {
        val lookupTag = expression.annotationTypeRef.coneType.classLikeLookupTagIfAny ?: return
        if (lookupTag.classId != jvmPackageNameClassId) return

        val nameValue = expression.getStringArgument(StandardNames.NAME, context.session) ?: return
        if (nameValue.isEmpty()) {
            reporter.reportOn(expression.source, FirJvmErrors.JVM_PACKAGE_NAME_CANNOT_BE_EMPTY, context)
        } else if (!isValidJavaFqName(nameValue)) {
            reporter.reportOn(expression.source, FirJvmErrors.JVM_PACKAGE_NAME_MUST_BE_VALID_NAME, context)
        }

        val file = context.containingFile ?: return
        @OptIn(DirectDeclarationsAccess::class)
        if (file.declarations.any { it is FirClass }) {
            reporter.reportOn(expression.source, FirJvmErrors.JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES, context)
        }
    }
}
