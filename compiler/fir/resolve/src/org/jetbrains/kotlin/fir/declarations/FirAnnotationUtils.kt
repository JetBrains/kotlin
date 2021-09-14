/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private fun FirAnnotation.toAnnotationLookupTag(): ConeClassLikeLookupTag? =
    // this cast fails when we have generic-typed annotations @T
    (annotationTypeRef.coneType as? ConeClassLikeType)?.lookupTag

fun FirAnnotation.toAnnotationClassId(): ClassId? =
    toAnnotationLookupTag()?.classId

private fun FirAnnotation.toAnnotationClass(session: FirSession): FirRegularClass? =
    toAnnotationLookupTag()?.toSymbol(session)?.fir as? FirRegularClass

// TODO: this is temporary solution, we need something better
private val FirExpression.callableNameOfMetaAnnotationArgument: Name?
    get() =
        (this as? FirQualifiedAccessExpression)?.let {
            val callableSymbol = (it.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirCallableSymbol<*>
            callableSymbol?.callableId?.callableName
        }

private val sourceName = Name.identifier("SOURCE")

fun FirAnnotationContainer.nonSourceAnnotations(session: FirSession): List<FirAnnotation> =
    annotations.filter { annotation ->
        val firAnnotationClass = annotation.toAnnotationClass(session)
        firAnnotationClass != null && firAnnotationClass.annotations.none { meta ->
            meta.toAnnotationClassId() == StandardClassIds.Annotations.Retention &&
                    meta.findArgumentByName(StandardClassIds.Annotations.ParameterNames.retentionValue)
                        ?.callableNameOfMetaAnnotationArgument == sourceName
        }
    }

inline val FirProperty.hasJvmFieldAnnotation: Boolean
    get() = annotations.any { it.isJvmFieldAnnotation }

val FirAnnotation.isJvmFieldAnnotation: Boolean
    get() = toAnnotationClassId() == StandardClassIds.Annotations.JvmField

fun FirAnnotation.useSiteTargetsFromMetaAnnotation(session: FirSession): Set<AnnotationUseSiteTarget> {
    return toAnnotationClass(session)
        ?.annotations
        ?.find { it.toAnnotationClassId() == StandardClassIds.Annotations.Target }
        ?.findArgumentByName(StandardClassIds.Annotations.ParameterNames.targetAllowedTargets)
        ?.unwrapVarargValue()
        ?.toAnnotationUseSiteTargets()
        ?: DEFAULT_USE_SITE_TARGETS
}

private fun List<FirExpression>.toAnnotationUseSiteTargets(): Set<AnnotationUseSiteTarget> =
    flatMapTo(mutableSetOf()) { arg ->
        when (val unwrappedArg = if (arg is FirNamedArgumentExpression) arg.expression else arg) {
            is FirArrayOfCall -> unwrappedArg.argumentList.arguments.toAnnotationUseSiteTargets()
            is FirVarargArgumentsExpression -> unwrappedArg.arguments.toAnnotationUseSiteTargets()
            else -> USE_SITE_TARGET_NAME_MAP[unwrappedArg.callableNameOfMetaAnnotationArgument?.identifier] ?: setOf()
        }
    }

// See [org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.USE_SITE_MAPPING] (it's in reverse)
private val USE_SITE_TARGET_NAME_MAP = mapOf(
    "FIELD" to setOf(AnnotationUseSiteTarget.FIELD, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD),
    "FILE" to setOf(AnnotationUseSiteTarget.FILE),
    "PROPERTY" to setOf(AnnotationUseSiteTarget.PROPERTY),
    "PROPERTY_GETTER" to setOf(AnnotationUseSiteTarget.PROPERTY_GETTER),
    "PROPERTY_SETTER" to setOf(AnnotationUseSiteTarget.PROPERTY_SETTER),
    "VALUE_PARAMETER" to setOf(
        AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER,
        AnnotationUseSiteTarget.RECEIVER,
        AnnotationUseSiteTarget.SETTER_PARAMETER,
    ),
)

// See [org.jetbrains.kotlin.descriptors.annotations.KotlinTarget] (the second argument of each entry)
private val DEFAULT_USE_SITE_TARGETS: Set<AnnotationUseSiteTarget> =
    USE_SITE_TARGET_NAME_MAP.values.fold(setOf<AnnotationUseSiteTarget>()) { a, b -> a + b } - setOf(AnnotationUseSiteTarget.FILE)

fun FirAnnotatedDeclaration.hasAnnotation(classId: ClassId): Boolean {
    return annotations.any { it.toAnnotationClassId() == classId }
}

fun <D> FirBasedSymbol<out D>.getAnnotationByClassId(classId: ClassId): FirAnnotation? where D : FirAnnotationContainer, D : FirDeclaration {
    return fir.getAnnotationByClassId(classId)
}

fun FirAnnotationContainer.getAnnotationByClassId(classId: ClassId): FirAnnotation? {
    return annotations.find {
        it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId == classId
    }
}

fun FirAnnotationContainer.getAnnotationsByClassId(classId: ClassId): List<FirAnnotation> = annotations.getAnnotationsByClassId(classId)

fun List<FirAnnotation>.getAnnotationsByClassId(classId: ClassId): List<FirAnnotation> {
    return filter {
        it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId == classId
    }
}

fun FirExpression.unwrapVarargValue(): List<FirExpression> {
    return when (this) {
        is FirVarargArgumentsExpression -> arguments
        is FirArrayOfCall -> arguments
        else -> listOf(this)
    }
}

fun FirAnnotation.findArgumentByName(name: Name): FirExpression? {
    argumentMapping.mapping[name]?.let { return it }
    if (this !is FirAnnotationCall) return null

    // NB: we have to consider both cases, because deserializer does not create argument mapping
    for (argument in arguments) {
        if (argument is FirNamedArgumentExpression && argument.name == name) {
            return argument.expression
        }
    }
    // I'm lucky today!
    // TODO: this line is still needed. However it should be replaced with 'return null'
    return arguments.singleOrNull()
}

fun FirAnnotation.getStringArgument(name: Name): String? =
    findArgumentByName(name)?.let { expression ->
        expression.safeAs<FirConstExpression<*>>()?.value as? String
    }

fun FirAnnotationContainer.getJvmNameFromAnnotation(target: AnnotationUseSiteTarget? = null): String? {
    val annotationCalls = getAnnotationsByClassId(StandardClassIds.Annotations.JvmName)
    return annotationCalls.firstNotNullOfOrNull { call ->
        call.getStringArgument(StandardClassIds.Annotations.ParameterNames.jvmNameName)
            ?.takeIf { target == null || call.useSiteTarget == target }
    }
}

val FirAnnotation.resolved: Boolean
    get() {
        if (annotationTypeRef !is FirResolvedTypeRef) return false
        if (this !is FirAnnotationCall) return true
        return calleeReference is FirResolvedNamedReference || calleeReference is FirErrorNamedReference
    }
