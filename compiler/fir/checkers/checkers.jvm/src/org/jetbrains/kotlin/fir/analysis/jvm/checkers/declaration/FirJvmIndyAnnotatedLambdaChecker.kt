/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirAnonymousFunctionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.getAnnotationRetention
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassLikeSymbol
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.isSuspendOrKSuspendFunctionType
import org.jetbrains.kotlin.name.JvmStandardClassIds

/**
 * INDY lambdas drop runtime annotations. Report a warning.
 *
 * See [LanguageFeature.JvmIndyAllowLambdasWithAnnotations].
 */
object FirJvmIndyAnnotatedLambdaChecker : FirAnonymousFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirAnonymousFunction) {
        if (!LanguageFeature.JvmIndyAllowLambdasWithAnnotations.isEnabled()) return

        // Inlined lambdas have no instance at all, this case is covered by NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION.
        if (declaration.inlineStatus == InlineStatus.Inline || declaration.inlineStatus == InlineStatus.CrossInline) return

        // Suspend lambdas are always compiled to a class, their annotations end up on 'invokeSuspend'.
        val isSuspend =
            if (declaration.isLambda) declaration.typeRef.coneTypeOrNull?.isSuspendOrKSuspendFunctionType(context.session) == true
            else declaration.isSuspend
        if (isSuspend) return

        // '@JvmSerializableLambda' forces the class generation scheme, so the annotations are retained.
        if (declaration.hasAnnotation(JvmStandardClassIds.JVM_SERIALIZABLE_LAMBDA_ANNOTATION_CLASS_ID, context.session)) return

        for (annotation in declaration.annotations) {
            val annotationClassSymbol = annotation.toAnnotationClassLikeSymbol(context.session) ?: continue
            if (annotationClassSymbol.getAnnotationRetention(context.session) != AnnotationRetention.RUNTIME) continue
            reporter.reportOn(
                annotation.source,
                FirJvmErrors.RUNTIME_ANNOTATION_ON_LAMBDA_IS_NOT_RETAINED,
                annotationClassSymbol,
            )
        }
    }
}
