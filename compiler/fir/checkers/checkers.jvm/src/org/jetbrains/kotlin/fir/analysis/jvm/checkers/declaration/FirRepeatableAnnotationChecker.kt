/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirAnnotatedDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.isJvm6
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object FirRepeatableAnnotationChecker : FirAnnotatedDeclarationChecker() {
    private val RETENTION_PARAMETER_NAME = Name.identifier("value")
    private val REPEATABLE_PARAMETER_NAME = Name.identifier("value")
    private val JAVA_REPEATABLE_ANNOTATION = FqName("java.lang.annotation.Repeatable")

    override fun check(declaration: FirAnnotatedDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotationsSet = hashSetOf<FqName>()

        val session = context.session
        val annotations = declaration.annotations
        for (annotation in annotations) {
            val classId = annotation.classId ?: continue
            val annotationClassId = annotation.toAnnotationClassId() ?: continue
            if (annotationClassId.isLocal) continue
            val annotationClass = session.symbolProvider.getClassLikeSymbolByClassId(annotationClassId) ?: continue

            // TODO: consider REPEATED_ANNOTATION ?
            if (fqName in annotationsSet &&
                annotationClass.isRepeatableAnnotation(session) &&
                annotationClass.getAnnotationRetention() != AnnotationRetention.SOURCE
            ) {
                if (context.isJvm6()) {
                    reporter.reportOn(annotation.source, FirJvmErrors.REPEATED_ANNOTATION_TARGET6, context)
                } else if (session.languageVersionSettings.supportsFeature(LanguageFeature.RepeatableAnnotations)) {
                    // It's not allowed to have both a repeated annotation (applied more than once) and its container
                    // on the same element.
                    // See https://docs.oracle.com/javase/specs/jls/se16/html/jls-9.html#jls-9.7.5.
                    val explicitContainer = annotationClass.resolveContainerAnnotation()
                    if (explicitContainer != null && annotations.any { it.fqName(session) == explicitContainer }) {
                        reporter.reportOn(
                            annotation.source,
                            FirJvmErrors.REPEATED_ANNOTATION_WITH_CONTAINER,
                            fqName,
                            explicitContainer,
                            context
                        )
                    }
                } else {
                    reporter.reportOn(annotation.source, FirJvmErrors.NON_SOURCE_REPEATED_ANNOTATION, context)
                }
            }

            annotationsSet.add(fqName)
        }
    }

    private fun FirClassLikeSymbol<*>.isRepeatableAnnotation(session: FirSession): Boolean {
        if (getAnnotationByFqName(StandardNames.FqNames.repeatable) != null) return true
        if (getAnnotationByFqName(JAVA_REPEATABLE_ANNOTATION) == null) return false

        return session.languageVersionSettings.supportsFeature(LanguageFeature.RepeatableAnnotations) ||
                getAnnotationRetention() == AnnotationRetention.SOURCE
    }

    private fun FirClassLikeSymbol<*>.getAnnotationRetention(): AnnotationRetention? {
        val repeatableAnnotation = getAnnotationByFqName(StandardNames.FqNames.retention) ?: return null
        val retentionValue = repeatableAnnotation.findArgumentByName(RETENTION_PARAMETER_NAME) ?: return null
        val resolvedSymbol = retentionValue.toResolvedCallableSymbol() as? FirEnumEntrySymbol ?: return null
        val name = resolvedSymbol.name.asString()

        return AnnotationRetention.values().firstOrNull { it.name == name }
    }

    private fun FirClassLikeSymbol<*>.resolveContainerAnnotation(): FqName? {
        val repeatableAnnotation =
            getAnnotationByFqName(StandardNames.FqNames.repeatable) ?: getAnnotationByFqName(JAVA_REPEATABLE_ANNOTATION) ?: return null
        val value = repeatableAnnotation.findArgumentByName(REPEATABLE_PARAMETER_NAME) ?: return null
        val classCallArgument = (value as? FirGetClassCall)?.argument ?: return null
        if (classCallArgument is FirResolvedQualifier) {
            return classCallArgument.relativeClassFqName
        } else if (classCallArgument is FirClassReferenceExpression) {
            val type = classCallArgument.classTypeRef.coneType.lowerBoundIfFlexible() as? ConeClassLikeType ?: return null
            return type.lookupTag.classId.asSingleFqName()
        }
        return null
    }
}