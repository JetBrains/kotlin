/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.*
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

fun FirAnnotation.toAnnotationClass(session: FirSession): FirRegularClass? =
    toAnnotationClassLikeSymbol(session)?.fir as? FirRegularClass

private val sourceName = Name.identifier("SOURCE")

fun List<FirAnnotation>.nonSourceAnnotations(session: FirSession): List<FirAnnotation> =
    this.filter { annotation ->
        val firAnnotationClass = annotation.toAnnotationClass(session)
        firAnnotationClass != null && firAnnotationClass.annotations.none { meta ->
            meta.toAnnotationClassId(session) == StandardClassIds.Annotations.Retention &&
                    meta.findArgumentByName(StandardClassIds.Annotations.ParameterNames.retentionValue)
                        ?.extractEnumValueArgumentInfo()?.enumEntryName == sourceName
        }
    }

fun FirAnnotationContainer.nonSourceAnnotations(session: FirSession): List<FirAnnotation> =
    annotations.nonSourceAnnotations(session)

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

fun FirExpression.unwrapVarargValue(): List<FirExpression> {
    return when (this) {
        is FirVarargArgumentsExpression -> when (val first = arguments.firstOrNull()) {
            is FirWrappedArgumentExpression -> first.expression.unwrapVarargValue()
            else -> arguments
        }
        is FirArrayLiteral -> arguments
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

    // The condition is required for annotation arguments that are not fully resolved. For example, CompilerRequiredAnnotations.
    // When the annotation is resolved, and we did not find an argument with the given name,
    // there is no argument and we should return null.
    return if (!resolved) arguments.firstOrNull() else null
}

fun FirAnnotation.getBooleanArgument(name: Name): Boolean? = getPrimitiveArgumentValue(name)
fun FirAnnotation.getStringArgument(name: Name): String? = getPrimitiveArgumentValue(name)
fun FirAnnotation.getStringArrayArgument(name: Name): List<String>? {
    val argument = findArgumentByName(name) as? FirArrayLiteral ?: return null
    return argument.arguments.mapNotNull { (it as? FirLiteralExpression<*>)?.value as? String }
}

private inline fun <reified T> FirAnnotation.getPrimitiveArgumentValue(name: Name): T? {
    return findArgumentByName(name)?.let { expression ->
        (expression as? FirLiteralExpression<*>)?.value as? T
    }
}

fun FirAnnotation.getKClassArgument(name: Name): ConeKotlinType? {
    val argument = findArgumentByName(name) as? FirGetClassCall ?: return null
    return argument.getTargetType()
}

fun FirGetClassCall.getTargetType(): ConeKotlinType? {
    return resolvedType.typeArguments.getOrNull(0)?.type
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

data class EnumValueArgumentInfo(val enumClassId: ClassId?, val enumEntryName: Name)

fun FirExpression.extractEnumValueArgumentInfo(): EnumValueArgumentInfo? {
    return when (this) {
        is FirPropertyAccessExpression -> {
            if (isResolved) {
                val entrySymbol = calleeReference.toResolvedEnumEntrySymbol() ?: return null
                EnumValueArgumentInfo(entrySymbol.callableId.classId!!, entrySymbol.callableId.callableName)
            } else {
                val enumEntryName = (calleeReference as? FirNamedReference)?.name ?: return null
                EnumValueArgumentInfo(null, enumEntryName)
            }
        }
        is FirEnumEntryDeserializedAccessExpression -> EnumValueArgumentInfo(enumClassId, enumEntryName)
        else -> null
    }
}
