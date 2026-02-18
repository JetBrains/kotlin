/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirEvaluatorResult
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedEnumEntrySymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapOr
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.PrivateForInline

// --------------------------- annotation -> type/class ---------------------------

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

// --------------------------- annotations lookup/filtering ---------------------------

private val sourceName: Name = Name.identifier("SOURCE")

fun List<FirAnnotation>.nonSourceAnnotations(session: FirSession): List<FirAnnotation> =
    this.filter { annotation ->
        val firAnnotationClass = annotation.toAnnotationClass(session)
        firAnnotationClass != null && firAnnotationClass.symbol.resolvedAnnotationsWithClassIds.none { meta ->
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

fun <D> FirBasedSymbol<D>.getAnnotationWithResolvedArgumentsByClassId(
    classId: ClassId,
    session: FirSession
): FirAnnotation? where D : FirAnnotationContainer, D : FirDeclaration {
    return resolvedAnnotationsWithArguments.getAnnotationByClassId(classId, session)
}

fun <D> FirBasedSymbol<D>.hasAnnotationWithClassId(
    classId: ClassId,
    session: FirSession
): Boolean where D : FirAnnotationContainer, D : FirDeclaration {
    return resolvedAnnotationsWithClassIds.getAnnotationByClassId(classId, session) != null
}

fun <D> FirBasedSymbol<D>.getAnnotationByClassId(
    classId: ClassId,
    session: FirSession
): FirAnnotation? where D : FirAnnotationContainer, D : FirDeclaration {
    return resolvedAnnotationsWithClassIds.getAnnotationByClassId(classId, session)
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
        it.doesMatchesClassId(classId, session)
    }
}

private fun FirAnnotation.doesMatchesClassId(
    classId: ClassId,
    session: FirSession,
): Boolean = annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.fullyExpandedType(session)?.classLikeLookupTagIfAny?.classId == classId

fun List<FirAnnotation>.filterOutAnnotationsByClassId(classId: ClassId, session: FirSession): List<FirAnnotation> {
    return filterNot {
        it.doesMatchesClassId(classId, session)
    }
}

fun List<FirAnnotation>.getAnnotationByClassIds(classIds: Collection<ClassId>, session: FirSession): FirAnnotation? {
    return firstOrNull {
        it.annotationTypeRef.coneTypeSafe<ConeKotlinType>()
            ?.fullyExpandedType(session)
            ?.classLikeLookupTagIfAny
            ?.classId in classIds
    }
}

// --------------------------- evaluated arguments ---------------------------

fun FirAnnotation.findArgumentByName(name: Name, returnFirstWhenNotFound: Boolean = true): FirExpression? {
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
    return if (!resolved && returnFirstWhenNotFound) arguments.firstOrNull() else null
}

@Deprecated(
    message = "Use getBooleanArgument overload without session parameter",
    replaceWith = ReplaceWith("getBooleanArgument(name)"),
    level = DeprecationLevel.HIDDEN
)
fun FirAnnotation.getBooleanArgument(name: Name, session: FirSession): Boolean? = getBooleanArgument(name)
fun FirAnnotation.getBooleanArgument(name: Name): Boolean? = getPrimitiveArgumentValue(name)

@Deprecated(
    message = "Use getStringArgument overload without session parameter",
    replaceWith = ReplaceWith("getStringArgument(name)"),
    level = DeprecationLevel.HIDDEN
)
fun FirAnnotation.getStringArgument(name: Name, session: FirSession): String? = getStringArgument(name)
fun FirAnnotation.getStringArgument(name: Name): String? = getPrimitiveArgumentValue(name)

private inline fun <reified T> FirAnnotation.getPrimitiveArgumentValue(name: Name): T? {
    val argument = findArgumentByName(name) ?: return null
    val literal = argument as? FirLiteralExpression ?: return null
    return literal.value as? T
}

@Deprecated(
    message = "Use getStringArrayArgument overload without session parameter",
    replaceWith = ReplaceWith("getStringArrayArgument(name)"),
    level = DeprecationLevel.HIDDEN
)
fun FirAnnotation.getStringArrayArgument(name: Name, session: FirSession): List<String>? = getStringArrayArgument(name)

fun FirAnnotation.getStringArrayArgument(name: Name): List<String>? {
    val argument = findArgumentByName(name) ?: return null
    val arrayLiteral = argument as? FirCollectionLiteral ?: return null
    return arrayLiteral.arguments.mapNotNull { (it as? FirLiteralExpression)?.value as? String }
}

@Deprecated(
    message = "Use getKClassArgument overload without session parameter",
    replaceWith = ReplaceWith("getKClassArgument(name)"),
    level = DeprecationLevel.HIDDEN
)
fun FirAnnotation.getKClassArgument(name: Name, session: FirSession): ConeKotlinType? = getKClassArgument(name)

fun FirAnnotation.getKClassArgument(name: Name): ConeKotlinType? {
    val argument = findArgumentByName(name) ?: return null
    val getClassCall = argument as? FirGetClassCall ?: return null
    return getClassCall.getTargetType()
}

fun FirGetClassCall.getTargetType(): ConeKotlinType? {
    return resolvedType.typeArguments.getOrNull(0)?.type
}

data class EnumValueArgumentInfo(val enumClassId: ClassId?, val enumEntryName: Name)

fun FirExpression.extractEnumValueArgumentInfo(): EnumValueArgumentInfo? {
    return when (this) {
        is FirPropertyAccessExpression -> {
            if (hasResolvedType) {
                val entrySymbol = calleeReference.toResolvedEnumEntrySymbol() ?: return null
                EnumValueArgumentInfo(entrySymbol.callableId.classId!!, entrySymbol.callableId.callableName)
            } else {
                val enumEntryName = calleeReference.name
                EnumValueArgumentInfo(null, enumEntryName)
            }
        }
        is FirEnumEntryDeserializedAccessExpression -> EnumValueArgumentInfo(enumClassId, enumEntryName)
        else -> null
    }
}

@OptIn(PrivateForInline::class, PrivateConstantEvaluatorAPI::class)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
inline fun <reified T : FirElement> FirExpression.evaluateAs(session: FirSession): @kotlin.internal.NoInfer T? {
    return FirExpressionEvaluator.evaluateExpression(this, session)?.unwrapOr<T> {}
}

// --------------------------- other utilities ---------------------------

fun FirExpression.unwrapVarargValue(): List<FirExpression> {
    return when (this) {
        is FirVarargArgumentsExpression -> when (val first = arguments.firstOrNull()) {
            is FirWrappedArgumentExpression -> first.expression.unwrapVarargValue()
            else -> arguments
        }
        is FirCollectionLiteral -> arguments
        else -> listOf(this)
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

fun hasLowPriorityAnnotation(annotations: List<FirAnnotation>): Boolean = annotations.any {
    val lookupTag = it.annotationTypeRef.coneType.classLikeLookupTagIfAny ?: return@any false
    lookupTag.classId == LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_CLASS_ID
}
