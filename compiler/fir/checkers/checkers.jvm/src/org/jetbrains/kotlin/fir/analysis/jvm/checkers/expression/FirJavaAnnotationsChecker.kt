/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassLikeSymbol
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.resolve.dfa.symbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.Annotations

object FirJavaAnnotationsChecker : FirAnnotationChecker() {

    private val javaToKotlinNameMap: Map<ClassId, ClassId> =
        mapOf(
            JvmStandardClassIds.Annotations.Java.Target to Annotations.Target,
            JvmStandardClassIds.Annotations.Java.Retention to Annotations.Retention,
            JvmStandardClassIds.Annotations.Java.Deprecated to Annotations.Deprecated,
            JvmStandardClassIds.Annotations.Java.Documented to Annotations.MustBeDocumented,
        )

    override fun check(expression: FirAnnotation, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.containingDeclarations.lastOrNull()?.source?.kind != KtRealSourceElementKind) return
        val callableSymbol = expression.annotationTypeRef.toClassLikeSymbol(context.session) as? FirClassSymbol<*> ?: return
        if (callableSymbol.origin !is FirDeclarationOrigin.Java) return

        val lookupTag = expression.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag ?: return
        javaToKotlinNameMap[lookupTag.classId]?.let { betterName ->
            reporter.reportOn(expression.source, FirJvmErrors.DEPRECATED_JAVA_ANNOTATION, betterName.asSingleFqName(), context)
        }

        if (expression is FirAnnotationCall) {
            val argumentList = expression.argumentList
            if (argumentList is FirResolvedArgumentList) {
                for ((key, value) in argumentList.mapping) {
                    if (value.name != Annotations.ParameterNames.value && key !is FirWrappedArgumentExpression) {
                        reporter.reportOn(key.source, FirJvmErrors.POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION, context)
                    }
                }
            }
        }
    }
}
