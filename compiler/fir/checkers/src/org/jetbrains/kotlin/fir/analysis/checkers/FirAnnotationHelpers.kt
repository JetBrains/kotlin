/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ParameterNames
import org.jetbrains.kotlin.resolve.UseSiteTargetsList
import org.jetbrains.kotlin.resolve.checkers.OptInNames

private val defaultAnnotationTargets = KotlinTarget.DEFAULT_TARGET_SET
private val defaultAnnotationTargetsWithExpression = KotlinTarget.DEFAULT_TARGET_SET + KotlinTarget.EXPRESSION

fun FirAnnotation.getAllowedAnnotationTargets(session: FirSession): Set<KotlinTarget> {
    if (annotationTypeRef is FirErrorTypeRef) return KotlinTarget.ALL_TARGET_SET
    val annotationClassSymbol = (this.annotationTypeRef.coneType as? ConeClassLikeType)
        ?.fullyExpandedType(session)?.lookupTag?.toSymbol(session) ?: return defaultAnnotationTargets
    annotationClassSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
    return annotationClassSymbol.getAllowedAnnotationTargets(session)
}

internal fun FirAnnotation.getAnnotationClassForOptInMarker(session: FirSession): FirRegularClassSymbol? {
    val lookupTag = annotationTypeRef.coneType.classLikeLookupTagIfAny ?: return null
    val annotationClassSymbol = lookupTag.toRegularClassSymbol(session) ?: return null
    if (!annotationClassSymbol.hasAnnotationWithClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID, session)) {
        return null
    }
    return annotationClassSymbol
}

fun FirRegularClass.getAllowedAnnotationTargets(session: FirSession): Set<KotlinTarget> {
    return symbol.getAllowedAnnotationTargets(session)
}

fun FirClassLikeSymbol<*>.getAllowedAnnotationTargets(session: FirSession): Set<KotlinTarget> {
    lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
    // In KT-67014, we decided to allow EXPRESSION targets for Java annotations
    val targetAnnotation = getTargetAnnotation(session)
        ?: return if (isJavaOrEnhancement) defaultAnnotationTargetsWithExpression else defaultAnnotationTargets
    val arguments =
        targetAnnotation.findArgumentByName(ParameterNames.targetAllowedTargets)?.unwrapAndFlattenArgument(flattenArrays = true).orEmpty()

    return arguments.mapNotNullTo(mutableSetOf()) { argument ->
        val targetName = argument.extractEnumValueArgumentInfo()?.enumEntryName?.asString() ?: return@mapNotNullTo null
        KotlinTarget.entries.firstOrNull { target -> target.name == targetName }
    }.let {
        // In KT-67014, we decided to allow EXPRESSION targets for Java annotations
        if (isJavaOrEnhancement) it + KotlinTarget.EXPRESSION else it
    }
}

fun FirDeclaration.getTargetAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationByClassId(StandardClassIds.Annotations.Target, session)
}

fun FirClassLikeSymbol<*>.getTargetAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationWithResolvedArgumentsByClassId(StandardClassIds.Annotations.Target, session)
}

fun FirExpression.extractClassesFromArgument(session: FirSession): List<FirRegularClassSymbol> {
    return unwrapAndFlattenArgument(flattenArrays = true).mapNotNull {
        it.extractClassFromArgument(session)
    }
}

fun FirExpression.extractClassFromArgument(session: FirSession): FirRegularClassSymbol? {
    if (this !is FirGetClassCall) return null
    return when (val argument = argument) {
        is FirResolvedQualifier ->
            argument.symbol?.fullyExpandedClass(session)
        is FirClassReferenceExpression -> {
            val classTypeRef = argument.classTypeRef
            val coneType = classTypeRef.coneType.unwrapToSimpleTypeUsingLowerBound()
            coneType.fullyExpandedType(session).toRegularClassSymbol(session)
        }
        else -> null
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkRepeatedAnnotation(
    useSiteTarget: AnnotationUseSiteTarget?,
    existingTargetsForAnnotation: MutableList<AnnotationUseSiteTarget?>,
    annotation: FirAnnotation,
    annotationSource: KtSourceElement?,
) {
    val duplicated = useSiteTarget in existingTargetsForAnnotation
            || existingTargetsForAnnotation.any { (it == null) != (useSiteTarget == null) }
    if (duplicated && !annotation.isRepeatable(context.session)) {
        reporter.reportOn(annotationSource, FirErrors.REPEATED_ANNOTATION)
    }
}

fun FirAnnotation.isRepeatable(session: FirSession): Boolean {
    val annotationClass = this.toAnnotationClassLikeSymbol(session) ?: return false
    if (annotationClass.isLocal) return false

    return session.annotationPlatformSupport.symbolContainsRepeatableAnnotation(annotationClass, session)
}

context(context: CheckerContext)
fun FirAnnotationContainer.getDefaultUseSiteTarget(
    annotation: FirAnnotation
): AnnotationUseSiteTarget? {
    return getImplicitUseSiteTargetList().firstOrNull {
        KotlinTarget.USE_SITE_MAPPING[it] in annotation.getAllowedAnnotationTargets(context.session)
    }
}

context(context: CheckerContext)
fun FirAnnotationContainer.getImplicitUseSiteTargetList(): List<AnnotationUseSiteTarget> {
    return when (this) {
        is FirValueParameter -> {
            if (context.findClosest<FirBasedSymbol<*>>().let { it is FirConstructorSymbol && it.isPrimary })
                UseSiteTargetsList.T_CONSTRUCTOR_PARAMETER
            else
                emptyList()
        }
        is FirProperty ->
            if (this.symbol is FirLocalPropertySymbol) emptyList() else UseSiteTargetsList.T_PROPERTY
        is FirPropertyAccessor ->
            if (isGetter) listOf(AnnotationUseSiteTarget.PROPERTY_GETTER) else listOf(AnnotationUseSiteTarget.PROPERTY_SETTER)
        else ->
            emptyList()
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkRepeatedAnnotation(
    annotationContainer: FirAnnotationContainer?,
    annotations: List<FirAnnotation>,
    annotationSources: Map<FirAnnotation, KtSourceElement?>,
    defaultSource: KtSourceElement?,
) {
    if (annotations.size <= 1) return

    val annotationsMap = hashMapOf<ConeKotlinType, MutableList<AnnotationUseSiteTarget?>>()

    for (annotation in annotations) {
        val useSiteTarget = annotation.useSiteTarget ?: annotationContainer?.getDefaultUseSiteTarget(annotation)
        val expandedType = annotation.annotationTypeRef.coneType.fullyExpandedType()
        val existingTargetsForAnnotation = annotationsMap.getOrPut(expandedType) { arrayListOf() }

        val source = annotationSources[annotation] ?: defaultSource
        checkRepeatedAnnotation(useSiteTarget, existingTargetsForAnnotation, annotation, source)
        existingTargetsForAnnotation.add(useSiteTarget)
    }
}

