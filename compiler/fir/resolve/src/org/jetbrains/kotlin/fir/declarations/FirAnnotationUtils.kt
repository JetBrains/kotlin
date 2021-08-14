/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private val RETENTION_CLASS_ID = ClassId.fromString("kotlin/annotation/Retention")
private val TARGET_CLASS_ID = ClassId.fromString("kotlin/annotation/Target")
private val JVM_FIELD_CLASS_ID = ClassId.fromString("kotlin/jvm/JvmField")

private fun FirAnnotationCall.toAnnotationLookupTag(): ConeClassLikeLookupTag =
    (annotationTypeRef.coneType as ConeClassLikeType).lookupTag

fun FirAnnotationCall.toAnnotationClassId(): ClassId =
    toAnnotationLookupTag().classId

private fun FirAnnotationCall.toAnnotationClass(session: FirSession): FirRegularClass? =
    toAnnotationLookupTag().toSymbol(session)?.fir as? FirRegularClass

// TODO: this is temporary solution, we need something better
private val FirExpression.callableNameOfMetaAnnotationArgument: Name?
    get() =
        (this as? FirQualifiedAccessExpression)?.let {
            val callableSymbol = (it.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirCallableSymbol<*>
            callableSymbol?.callableId?.callableName
        }

fun FirAnnotationContainer.nonSourceAnnotations(session: FirSession): List<FirAnnotationCall> =
    annotations.filter { annotation ->
        val firAnnotationClass = annotation.toAnnotationClass(session)
        firAnnotationClass != null && firAnnotationClass.annotations.none { meta ->
            meta.toAnnotationClassId() == RETENTION_CLASS_ID &&
                    meta.argumentList.arguments.singleOrNull()?.callableNameOfMetaAnnotationArgument == Name.identifier("SOURCE")
        }
    }

inline val FirProperty.hasJvmFieldAnnotation: Boolean
    get() = annotations.any { it.isJvmFieldAnnotation }

val FirAnnotationCall.isJvmFieldAnnotation: Boolean
    get() = toAnnotationClassId() == JVM_FIELD_CLASS_ID

fun FirAnnotationCall.useSiteTargetsFromMetaAnnotation(session: FirSession): Set<AnnotationUseSiteTarget> =
    toAnnotationClass(session)?.annotations?.find { it.toAnnotationClassId() == TARGET_CLASS_ID }?.argumentList?.arguments
        ?.toAnnotationUseSiteTargets() ?: DEFAULT_USE_SITE_TARGETS

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

fun FirAnnotationContainer.getAnnotationByFqName(fqName: FqName): FirAnnotationCall? {
    return annotations.find {
        it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId?.asSingleFqName() == fqName
    }
}

fun <D> FirBasedSymbol<out D>.getAnnotationByFqName(fqName: FqName): FirAnnotationCall? where D : FirAnnotationContainer, D : FirDeclaration {
    return fir.getAnnotationByFqName(fqName)
}

fun FirAnnotationContainer.getAnnotationsByFqName(fqName: FqName): List<FirAnnotationCall> = annotations.getAnnotationsByFqName(fqName)

fun List<FirAnnotationCall>.getAnnotationsByFqName(fqName: FqName): List<FirAnnotationCall> {
    return filter {
        it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId?.asSingleFqName() == fqName
    }
}

fun FirAnnotationCall.findArgumentByName(name: Name): FirExpression? {
    val argumentMapping = argumentMapping
    if (argumentMapping != null) {
        return argumentMapping.keys.find { argumentMapping[it]?.name == name }?.unwrapArgument()
    }
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

fun FirAnnotationCall.getStringArgument(name: Name): String? =
    findArgumentByName(name)?.let { expression ->
        expression.safeAs<FirConstExpression<*>>()?.value as? String
    }

private val jvmNameAnnotationFqName = FqName.fromSegments(listOf("kotlin", "jvm", "JvmName"))

fun FirAnnotationContainer.getJvmNameFromAnnotation(target: AnnotationUseSiteTarget? = null): String? {
    val name = Name.identifier("name")
    val annotationCalls = getAnnotationsByFqName(jvmNameAnnotationFqName)
    return annotationCalls.firstNotNullOfOrNull { call ->
        call.getStringArgument(name)?.takeIf { target == null || call.useSiteTarget == target }
    }
}