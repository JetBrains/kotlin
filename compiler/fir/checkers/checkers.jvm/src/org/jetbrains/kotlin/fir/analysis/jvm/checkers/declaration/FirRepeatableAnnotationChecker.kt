/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.getSingleClassifier
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object FirRepeatableAnnotationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    private val REPEATABLE_ANNOTATION_CONTAINER_NAME = Name.identifier(JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val annotations = declaration.annotations
        if (annotations.isEmpty()) return
        val annotationsMap = hashMapOf<ConeKotlinType, MutableList<AnnotationUseSiteTarget?>>()

        val session = context.session
        for (annotation in annotations) {
            val unexpandedClassId = annotation.unexpandedClassId ?: continue
            val annotationClass = annotation.toAnnotationClassLikeSymbol(session) ?: continue
            if (annotationClass.isLocal) continue

            val useSiteTarget = annotation.useSiteTarget
            val expandedType = annotation.annotationTypeRef.coneType.fullyExpandedType()
            val existingTargetsForAnnotation = annotationsMap.getOrPut(expandedType) { arrayListOf() }
            val duplicateAnnotation = useSiteTarget in existingTargetsForAnnotation ||
                    existingTargetsForAnnotation.any { (it == null) != (useSiteTarget == null) }

            if (duplicateAnnotation &&
                session.annotationPlatformSupport.symbolContainsRepeatableAnnotation(annotationClass, session) &&
                annotationClass.getAnnotationRetention(session) != AnnotationRetention.SOURCE
            ) {
                // It's not allowed to have both a repeated annotation (applied more than once) and its container
                // on the same element. See https://docs.oracle.com/javase/specs/jls/se16/html/jls-9.html#jls-9.7.5.
                val explicitContainer = annotationClass.resolveContainerAnnotation(session)
                if (explicitContainer != null && annotations.any { it.toAnnotationClassId(session) == explicitContainer }) {
                    reporter.reportOn(
                        annotation.source,
                        FirJvmErrors.REPEATED_ANNOTATION_WITH_CONTAINER,
                        unexpandedClassId,
                        explicitContainer
                    )
                }
            }

            existingTargetsForAnnotation.add(useSiteTarget)
        }

        if (declaration is FirRegularClass) {
            val javaRepeatable = annotations.getAnnotationByClassId(JvmStandardClassIds.Annotations.Java.Repeatable, session)
            if (javaRepeatable != null) {
                checkJavaRepeatableAnnotationDeclaration(javaRepeatable, declaration)
            } else {
                val kotlinRepeatable = annotations.getAnnotationByClassId(StandardClassIds.Annotations.Repeatable, session)
                if (kotlinRepeatable != null) {
                    checkKotlinRepeatableAnnotationDeclaration(kotlinRepeatable, declaration)
                }
            }
        }
    }

    private fun FirClassLikeSymbol<*>.resolveContainerAnnotation(session: FirSession): ClassId? {
        val repeatableAnnotation = getAnnotationWithResolvedArgumentsByClassId(StandardClassIds.Annotations.Repeatable, session)
            ?: getAnnotationWithResolvedArgumentsByClassId(JvmStandardClassIds.Annotations.Java.Repeatable, session)
            ?: return null
        return repeatableAnnotation.resolveContainerAnnotation()
    }

    private fun FirAnnotation.resolveContainerAnnotation(): ClassId? {
        val value = findArgumentByName(StandardClassIds.Annotations.ParameterNames.value) ?: return null
        val classCallArgument = (value as? FirGetClassCall)?.argument ?: return null
        if (classCallArgument is FirResolvedQualifier) {
            return classCallArgument.classId
        } else if (classCallArgument is FirClassReferenceExpression) {
            val type = classCallArgument.classTypeRef.coneType.lowerBoundIfFlexible() as? ConeClassLikeType ?: return null
            return type.lookupTag.classId
        }
        return null
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkJavaRepeatableAnnotationDeclaration(
        javaRepeatable: FirAnnotation,
        annotationClass: FirRegularClass,
    ) {
        val containerClassId = javaRepeatable.resolveContainerAnnotation() ?: return
        val containerClassSymbol =
            context.session.symbolProvider.getClassLikeSymbolByClassId(containerClassId) as? FirRegularClassSymbol ?: return

        checkRepeatableAnnotationContainer(annotationClass, containerClassSymbol, javaRepeatable.source)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkKotlinRepeatableAnnotationDeclaration(
        kotlinRepeatable: FirAnnotation,
        declaration: FirRegularClass,
    ) {
        val unsubsitutedScope = declaration.unsubstitutedScope()
        if (unsubsitutedScope.getSingleClassifier(REPEATABLE_ANNOTATION_CONTAINER_NAME) != null) {
            reporter.reportOn(kotlinRepeatable.source, FirJvmErrors.REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER_ERROR)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkRepeatableAnnotationContainer(
        annotationClass: FirRegularClass,
        containerClass: FirRegularClassSymbol,
        annotationSource: KtSourceElement?,
    ) {
        checkContainerParameters(containerClass, annotationClass, annotationSource)
        checkContainerRetention(containerClass, annotationClass, annotationSource)
        checkContainerTarget(containerClass, annotationClass, annotationSource)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkContainerParameters(
        containerClass: FirRegularClassSymbol,
        annotationClass: FirRegularClass,
        annotationSource: KtSourceElement?,
    ) {
        val session = context.session
        val containerCtor = containerClass.primaryConstructorIfAny(session) ?: return

        val valueParameterSymbols = containerCtor.valueParameterSymbols
        val parameterName = StandardClassIds.Annotations.ParameterNames.value
        val value = valueParameterSymbols.find { it.name == parameterName }
        val fullyExpandedType = value?.resolvedReturnTypeRef?.coneType?.fullyExpandedType()
        if (fullyExpandedType == null ||
            !fullyExpandedType.isArrayType ||
            fullyExpandedType.typeArguments.single().type != annotationClass.defaultType()
        ) {
            reporter.reportOn(
                annotationSource,
                FirJvmErrors.REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY_ERROR,
                containerClass.classId,
                annotationClass.classId
            )
            return
        }

        val otherNonDefault = valueParameterSymbols.find { it.name != parameterName && !it.hasDefaultValue }
        if (otherNonDefault != null) {
            reporter.reportOn(
                annotationSource,
                FirJvmErrors.REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER_ERROR,
                containerClass.classId,
                otherNonDefault.name
            )
            return
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkContainerRetention(
        containerClass: FirRegularClassSymbol,
        annotationClass: FirRegularClass,
        annotationSource: KtSourceElement?,
    ) {
        val annotationRetention = annotationClass.symbol.getAnnotationRetention(context.session)
        val containerRetention = containerClass.getAnnotationRetention(context.session)
        if (containerRetention < annotationRetention) {
            reporter.reportOn(
                annotationSource,
                FirJvmErrors.REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION_ERROR,
                containerClass.classId,
                containerRetention.name,
                annotationClass.classId,
                annotationRetention.name
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkContainerTarget(
        containerClass: FirRegularClassSymbol,
        annotationClass: FirRegularClass,
        annotationSource: KtSourceElement?,
    ) {
        val annotationTargets = annotationClass.getAllowedAnnotationTargets(context.session)
        val containerTargets = containerClass.getAllowedAnnotationTargets(context.session)

        // See https://docs.oracle.com/javase/specs/jls/se16/html/jls-9.html#jls-9.6.3.
        // (TBH, the rules about TYPE/TYPE_USE and TYPE_PARAMETER/TYPE_USE don't seem to make a lot of sense, but it's JLS
        // so we better obey it for full interop with the Java language and reflection.)
        for (target in containerTargets) {
            val ok = when (target) {
                in annotationTargets -> true
                KotlinTarget.ANNOTATION_CLASS ->
                    KotlinTarget.CLASS in annotationTargets ||
                            KotlinTarget.TYPE in annotationTargets
                KotlinTarget.CLASS ->
                    KotlinTarget.TYPE in annotationTargets
                KotlinTarget.TYPE_PARAMETER ->
                    KotlinTarget.TYPE in annotationTargets
                else -> false
            }
            if (!ok) {
                reporter.reportOn(
                    annotationSource,
                    FirJvmErrors.REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET_ERROR,
                    containerClass.classId,
                    annotationClass.classId
                )
                return
            }
        }
    }
}
