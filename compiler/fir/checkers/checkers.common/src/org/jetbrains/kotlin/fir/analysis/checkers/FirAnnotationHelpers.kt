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
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.UseSiteTargetsList
import org.jetbrains.kotlin.resolve.checkers.OptInNames

internal fun FirAnnotation.getAnnotationClassForOptInMarker(session: FirSession): FirRegularClassSymbol? {
    val lookupTag = annotationTypeRef.coneType.classLikeLookupTagIfAny ?: return null
    val annotationClassSymbol = lookupTag.toRegularClassSymbol(session) ?: return null
    if (!annotationClassSymbol.hasAnnotationWithClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID, session)) {
        return null
    }
    return annotationClassSymbol
}

fun FirExpression.extractClassesFromArgument(session: FirSession): List<FirRegularClassSymbol> {
    return unwrapAndFlattenArgument(flattenArrays = true).mapNotNull {
        it.extractClassFromArgument(session)
    }
}

fun FirExpression.extractClassesAndSourcesFromArgument(session: FirSession): List<Pair<FirRegularClassSymbol, KtSourceElement?>> {
    return unwrapAndFlattenArgument(flattenArrays = true).mapNotNull {
        it.extractClassFromArgument(session)?.to(it.source)
    }
}

fun FirExpression.extractClassFromArgument(session: FirSession): FirRegularClassSymbol? {
    if (this !is FirGetClassCall) return null
    return when (val argument = argument) {
        is FirResolvedQualifier ->
            argument.qualifierSymbol?.fullyExpandedClass(session)
        is FirClassReferenceExpression -> {
            val classTypeRef = argument.classTypeRef
            val coneType = classTypeRef.coneType.unwrapToSimpleTypeUsingLowerBound()
            coneType.fullyExpandedType(session).toRegularClassSymbol(session)
        }
        else -> null
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkRepeatedAnnotation(annotation: FirAnnotation, annotationSource: KtSourceElement?) {
    if (!annotation.isRepeatable(context.session)) {
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
    annotations: List<FirAnnotation>,
    annotationSources: Map<FirAnnotation, KtSourceElement?>,
    defaultSource: KtSourceElement?,
) {
    if (annotations.size <= 1) return
    val annotationsSet = hashSetOf<ConeKotlinType>()

    for (annotation in annotations) {
        val expandedType = annotation.annotationTypeRef.coneType.fullyExpandedType()
        val source = annotationSources[annotation] ?: defaultSource
        if (expandedType in annotationsSet) {
            checkRepeatedAnnotation(annotation, source)
        }
        annotationsSet.add(expandedType)
    }
}

/**
 * If [this] is an argument expression provided for a vararg parameter, returns its elements.
 *
 * Only allowed to be called on evaluated arguments from [FirAnnotation.argumentMapping]s.
 */
fun FirExpression.unwrapVarargValue(): List<FirExpression> {
    return when (this) {
        is FirVarargArgumentsExpression -> arguments

        // This branch is necessary because of how serialization / deserialization works:
        // When deserializing an argument for vararg, it is deserialized as `FirCollectionLiteral`, not `FirVarargArgumentsExpression`.
        is FirCollectionLiteral -> arguments
        else -> listOf()
    }
}
