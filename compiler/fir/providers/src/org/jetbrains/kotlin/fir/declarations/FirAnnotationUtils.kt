/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

fun FirAnnotation.toAnnotationClassLikeType(session: FirSession): ConeClassLikeType? =
    // this cast fails when we have generic-typed annotations @T
    (annotationTypeRef.coneType as? ConeClassLikeType)?.fullyExpandedType(session)

private fun FirAnnotation.toAnnotationLookupTag(session: FirSession): ConeClassLikeLookupTag? =
    toAnnotationClassLikeType(session)?.lookupTag

private fun FirAnnotation.toAnnotationLookupTagSafe(session: FirSession): ConeClassLikeLookupTag? =
    annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.fullyExpandedType(session)?.lookupTag

fun FirAnnotation.toAnnotationClassId(session: FirSession): ClassId? =
    toAnnotationLookupTag(session)?.classId

fun FirAnnotation.toAnnotationClassIdSafe(session: FirSession): ClassId? =
    toAnnotationLookupTagSafe(session)?.classId

fun FirAnnotation.toAnnotationClassLikeSymbol(session: FirSession): FirClassLikeSymbol<*>? =
    toAnnotationLookupTag(session)?.toSymbol(session)

private fun FirAnnotation.toAnnotationClass(session: FirSession): FirRegularClass? =
    toAnnotationClassLikeSymbol(session)?.fir as? FirRegularClass

// TODO: this is temporary solution, we need something better
private val FirExpression.callableNameOfMetaAnnotationArgument: Name?
    get() =
        (this as? FirQualifiedAccessExpression)?.let {
            val callableSymbol = it.calleeReference.toResolvedCallableSymbol()
            callableSymbol?.callableId?.callableName
        }

private val sourceName = Name.identifier("SOURCE")

fun List<FirAnnotation>.nonSourceAnnotations(session: FirSession): List<FirAnnotation> =
    this.filter { annotation ->
        val firAnnotationClass = annotation.toAnnotationClass(session)
        firAnnotationClass != null && firAnnotationClass.annotations.none { meta ->
            meta.toAnnotationClassId(session) == StandardClassIds.Annotations.Retention &&
                    meta.findArgumentByName(StandardClassIds.Annotations.ParameterNames.retentionValue)
                        ?.callableNameOfMetaAnnotationArgument == sourceName
        }
    }

fun FirAnnotationContainer.nonSourceAnnotations(session: FirSession): List<FirAnnotation> =
    annotations.nonSourceAnnotations(session)

@Suppress("NOTHING_TO_INLINE")
inline fun FirProperty.hasJvmFieldAnnotation(session: FirSession): Boolean = annotations.any { it.isJvmFieldAnnotation(session) }

fun FirAnnotation.isJvmFieldAnnotation(session: FirSession): Boolean =
    toAnnotationClassId(session) == StandardClassIds.Annotations.JvmField

fun FirAnnotation.useSiteTargetsFromMetaAnnotation(session: FirSession): Set<AnnotationUseSiteTarget> {
    return toAnnotationClass(session)
        ?.annotations
        ?.find { it.toAnnotationClassIdSafe(session) == StandardClassIds.Annotations.Target }
        ?.findUseSiteTargets()
        ?: DEFAULT_USE_SITE_TARGETS
}

private fun FirAnnotation.findUseSiteTargets(): Set<AnnotationUseSiteTarget> = buildSet {
    fun addIfMatching(arg: FirExpression) {
        if (arg !is FirQualifiedAccessExpression) return
        val callableSymbol = arg.calleeReference.toResolvedCallableSymbol() ?: return
        if (callableSymbol.containingClassLookupTag()?.classId == StandardClassIds.AnnotationTarget) {
            USE_SITE_TARGET_NAME_MAP[callableSymbol.callableId.callableName.identifier]?.let { addAll(it) }
        }
    }

    if (this@findUseSiteTargets is FirAnnotationCall) {
        for (arg in argumentList.arguments) {
            arg.unwrapAndFlattenArgument().forEach(::addIfMatching)
        }
    } else {
        argumentMapping.mapping[StandardClassIds.Annotations.ParameterNames.targetAllowedTargets]
            ?.unwrapAndFlattenArgument()
            ?.forEach(::addIfMatching)
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

fun FirDeclaration.hasAnnotation(type: ConeClassLikeType, session: FirSession): Boolean {
    return annotations.hasAnnotation(type, session)
}

fun FirDeclaration.hasAnnotation(classId: ClassId, session: FirSession): Boolean {
    return annotations.hasAnnotation(classId, session)
}

fun FirDeclaration.hasAnnotationSafe(classId: ClassId, session: FirSession): Boolean {
    return annotations.hasAnnotationSafe(classId, session)
}

fun FirBasedSymbol<*>.hasAnnotation(classId: ClassId, session: FirSession): Boolean {
    return resolvedAnnotationsWithClassIds.hasAnnotation(classId, session)
}

fun FirAnnotationContainer.hasAnnotation(classId: ClassId, session: FirSession): Boolean {
    return annotations.hasAnnotation(classId, session)
}

fun List<FirAnnotation>.hasAnnotation(type: ConeClassLikeType, session: FirSession): Boolean {
    return this.any { it.toAnnotationClassLikeType(session) == type }
}

fun List<FirAnnotation>.hasAnnotation(classId: ClassId, session: FirSession): Boolean {
    return this.any { it.toAnnotationClassId(session) == classId }
}

fun List<FirAnnotation>.hasAnnotationSafe(classId: ClassId, session: FirSession): Boolean {
    return this.any { it.toAnnotationClassIdSafe(session) == classId }
}

fun <D> FirBasedSymbol<out D>.getAnnotationByClassId(
    classId: ClassId,
    session: FirSession
): FirAnnotation? where D : FirAnnotationContainer, D : FirDeclaration {
    return fir.getAnnotationByClassId(classId, session)
}

fun FirAnnotationContainer.getAnnotationByClassId(classId: ClassId, session: FirSession): FirAnnotation? {
    return annotations.getAnnotationByClassId(classId, session)
}

fun List<FirAnnotation>.getAnnotationByClassId(classId: ClassId, session: FirSession): FirAnnotation? {
    return getAnnotationsByClassId(classId, session).firstOrNull()
}

fun FirAnnotationContainer.getAnnotationsByClassId(classId: ClassId, session: FirSession): List<FirAnnotation> =
    annotations.getAnnotationsByClassId(classId, session)

fun List<FirAnnotation>.getAnnotationsByClassId(classId: ClassId, session: FirSession): List<FirAnnotation> {
    return filter {
        it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.fullyExpandedType(session)?.lookupTag?.classId == classId
    }
}

fun List<FirAnnotation>.getAnnotationByClassIds(classIds: Collection<ClassId>, session: FirSession): FirAnnotation? {
    return firstOrNull {
        it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.fullyExpandedType(session)?.lookupTag?.classId in classIds
    }
}

inline fun <T> List<FirAnnotation>.mapAnnotationsWithClassIdTo(
    classId: ClassId,
    destination: MutableCollection<T>,
    func: (FirAnnotation) -> T
) {
    for (annotation in this) {
        if (annotation.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId == classId) {
            destination.add(func(annotation))
        }
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

fun FirAnnotation.getBooleanArgument(name: Name): Boolean? = getPrimitiveArgumentValue(name)
fun FirAnnotation.getStringArgument(name: Name): String? = getPrimitiveArgumentValue(name)
fun FirAnnotation.getIntArgument(name: Name): Int? = getPrimitiveArgumentValue(name)
fun FirAnnotation.getStringArrayArgument(name: Name): List<String>? {
    val argument = findArgumentByName(name) as? FirArrayOfCall ?: return null
    return argument.arguments.mapNotNull { (it as? FirConstExpression<*>)?.value as? String }
}

private inline fun <reified T> FirAnnotation.getPrimitiveArgumentValue(name: Name): T? {
    return findArgumentByName(name)?.let { expression ->
        (expression as? FirConstExpression<*>)?.value as? T
    }
}

fun FirAnnotation.getKClassArgument(name: Name): ConeKotlinType? {
    val argument = findArgumentByName(name) as? FirGetClassCall ?: return null
    return argument.getTargetType()
}

fun FirGetClassCall.getTargetType(): ConeKotlinType? {
    return typeRef.coneType.typeArguments.getOrNull(0)?.type
}

fun FirAnnotationContainer.getJvmNameFromAnnotation(session: FirSession, target: AnnotationUseSiteTarget? = null): String? {
    val annotationCalls = getAnnotationsByClassId(StandardClassIds.Annotations.JvmName, session)
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

private val LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_CLASS_ID: ClassId =
    ClassId(FqName("kotlin.internal"), Name.identifier("LowPriorityInOverloadResolution"))

fun hasLowPriorityAnnotation(annotations: List<FirAnnotation>) = annotations.any {
    val lookupTag = it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag ?: return@any false
    lookupTag.classId == LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_CLASS_ID
}
