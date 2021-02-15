/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.resolve.getCorrespondingClassSymbolOrNull
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

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
private val FirExpression.enumEntryName: Name?
    get() = ((this as? FirQualifiedAccessExpression)?.calleeReference as? FirNamedReference)?.name

fun FirAnnotationContainer.nonSourceAnnotations(session: FirSession): List<FirAnnotationCall> =
    annotations.filter { annotation ->
        val firAnnotationClass = annotation.toAnnotationClass(session)
        firAnnotationClass != null && firAnnotationClass.annotations.none { meta ->
            meta.toAnnotationClassId() == RETENTION_CLASS_ID &&
                    meta.argumentList.arguments.singleOrNull()?.enumEntryName == Name.identifier("SOURCE")
        }
    }

inline val FirProperty.hasJvmFieldAnnotation: Boolean
    get() = annotations.any { it.isJvmFieldAnnotation }

val FirAnnotationCall.isJvmFieldAnnotation: Boolean
    get() = toAnnotationClassId() == JVM_FIELD_CLASS_ID

fun FirAnnotationCall.useSiteTargetsFromMetaAnnotation(session: FirSession): Set<AnnotationUseSiteTarget> =
    toAnnotationClass(session)?.annotations?.find { it.toAnnotationClassId() == TARGET_CLASS_ID }?.argumentList?.arguments
        ?.toAnnotationUseSiteTargets() ?: DEFAULT_USE_SITE_TARGETS

private val FirExpression.unwrapNamedArgument: FirExpression
    get() = if (this is FirNamedArgumentExpression) expression else this

private fun List<FirExpression>.toAnnotationUseSiteTargets(): Set<AnnotationUseSiteTarget> =
    flatMapTo(mutableSetOf()) { arg ->
        when (val unwrappedArg = arg.unwrapNamedArgument) {
            is FirArrayOfCall -> unwrappedArg.argumentList.arguments.toAnnotationUseSiteTargets()
            is FirVarargArgumentsExpression -> unwrappedArg.arguments.toAnnotationUseSiteTargets()
            else -> USE_SITE_TARGET_NAME_MAP[unwrappedArg.enumEntryName?.identifier] ?: setOf()
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

private val DEPRECATED_ANNOTATION = ClassId(FqName("kotlin"), Name.identifier("Deprecated"))

fun FirAnnotatedDeclaration.deprecationStatus(): DeprecationLevel? {
    for (annotation in annotations) {
        // TODO: also check Kotlin version annotations
        // TODO: to correctly determine visibility of star-imported classifiers in `FirAbstractImportingScope`,
        //   types of annotations need to be resolved in topological order, or else this breaks.
        if (annotation.toAnnotationClassId() != DEPRECATED_ANNOTATION) continue

        val deprecatedClass = annotation.getCorrespondingClassSymbolOrNull(session)!!
        val deprecatedConstructor = deprecatedClass.fir.declarations.single { it is FirConstructor } as FirConstructor
        val parameterMapping = annotation.argumentMapping?.entries?.associate { it.value to it.key }
            ?: annotation.arguments.withIndex().associate { (index, argument) ->
                // TODO: this is super fragile, better resolve annotations before everything else
                val parameter = if (argument is FirNamedArgumentExpression)
                    deprecatedConstructor.valueParameters.find { it.name == argument.name }
                else
                    deprecatedConstructor.valueParameters.getOrNull(index)
                parameter to argument
            }
        val (_ /*message*/, _ /*replaceWith*/, level) = deprecatedConstructor.valueParameters
        // TODO: return the message too (constant string, no default)
        return when (parameterMapping[level]?.unwrapNamedArgument?.enumEntryName?.asString()) {
            "ERROR" -> DeprecationLevel.ERROR
            "HIDDEN" -> DeprecationLevel.HIDDEN
            else -> DeprecationLevel.WARNING
        }
    }
    return null
}
