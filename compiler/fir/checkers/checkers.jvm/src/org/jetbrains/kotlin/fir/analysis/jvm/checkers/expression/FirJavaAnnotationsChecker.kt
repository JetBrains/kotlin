/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.descriptors.isAnnotationClass
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.Annotations

object FirJavaAnnotationsChecker : FirAnnotationChecker(MppCheckerKind.Common) {

    private val javaToKotlinNameMap: Map<ClassId, ClassId> =
        mapOf(
            JvmStandardClassIds.Annotations.Java.Target to Annotations.Target,
            JvmStandardClassIds.Annotations.Java.Retention to Annotations.Retention,
            JvmStandardClassIds.Annotations.Java.Deprecated to Annotations.Deprecated,
            JvmStandardClassIds.Annotations.Java.Documented to Annotations.MustBeDocumented,
        )

    private val allowedSourceKinds = setOf(KtRealSourceElementKind, KtFakeSourceElementKind.FromUseSiteTarget)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirAnnotation) {
        if (expression.source?.kind !in allowedSourceKinds) return
        val annotationType = expression.annotationTypeRef.coneType.abbreviatedTypeOrSelf
        val classSymbol = annotationType.classLikeLookupTagIfAny?.toClassSymbol() ?: return
        if (classSymbol.origin !is FirDeclarationOrigin.Java) return

        val lookupTag = classSymbol.toLookupTag()
        javaToKotlinNameMap[lookupTag.classId]?.let { betterName ->
            reporter.reportOn(expression.source, FirJvmErrors.DEPRECATED_JAVA_ANNOTATION, betterName.asSingleFqName())
        }

        if (expression is FirAnnotationCall) {
            checkArgumentList(expression.argumentList)
        }
    }
}

object FirJavaAnnotationsConstructorCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val symbol = expression.toResolvedCallableSymbol() ?: return
        if (symbol !is FirConstructorSymbol) return
        val classSymbol = symbol.getContainingClassSymbol() ?: return
        if (classSymbol.origin !is FirDeclarationOrigin.Java) return
        if (classSymbol.classKind?.isAnnotationClass != true) return

        checkArgumentList(expression.argumentList)
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkArgumentList(argumentList: FirArgumentList) {
    if (argumentList is FirResolvedArgumentList) {
        val arguments = argumentList.originalArgumentList?.arguments ?: return
        for (key in arguments) {
            if (key !is FirWrappedArgumentExpression &&
                key !is FirErrorExpression &&
                argumentList.mapping[key]?.name.let { it != null && it != Annotations.ParameterNames.value }
            ) {
                reporter.reportOn(key.source, FirJvmErrors.POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION)
            }
        }
    }
}
