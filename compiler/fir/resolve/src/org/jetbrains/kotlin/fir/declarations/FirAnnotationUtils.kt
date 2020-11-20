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
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
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

private fun FirAnnotationCall.useSiteTargetsFromMetaAnnotation(session: FirSession): List<AnnotationUseSiteTarget> {
    val metaAnnotationAboutTarget =
        toAnnotationClass(session)?.annotations?.find { it.toAnnotationClassId() == TARGET_CLASS_ID }
            ?: return emptyList()
    return metaAnnotationAboutTarget.argumentList.arguments.toAnnotationUseSiteTargets()
}

private fun List<FirExpression>.toAnnotationUseSiteTargets(): List<AnnotationUseSiteTarget> =
    flatMap { arg ->
        val unwrappedArg = if (arg is FirNamedArgumentExpression) arg.expression else arg
        if (unwrappedArg is FirArrayOfCall) {
            unwrappedArg.argumentList.arguments.toAnnotationUseSiteTargets()
        } else {
            unwrappedArg.toAnnotationUseSiteTarget()?.let { listOf(it) } ?: emptyList()
        }
    }

private val USE_SITE_TARGET_NAME_MAP =
    AnnotationUseSiteTarget.values().map { it.name to it }.toMap()

private fun FirExpression.toAnnotationUseSiteTarget(): AnnotationUseSiteTarget? =
    // TODO: depending on the context, "PARAMETER" can be mapped to either CONSTRUCTOR_PARAMETER or SETTER_PARAMETER ?
    callableNameOfMetaAnnotationArgument?.identifier?.let {
        USE_SITE_TARGET_NAME_MAP[it]
    }

fun FirAnnotationCall.hasMetaAnnotationUseSiteTargets(session: FirSession, vararg useSiteTargets: AnnotationUseSiteTarget): Boolean {
    val meta = useSiteTargetsFromMetaAnnotation(session)
    return useSiteTargets.any { meta.contains(it) }
}

fun FirAnnotatedDeclaration.hasAnnotation(classId: ClassId): Boolean {
    return annotations.any { it.toAnnotationClassId() == classId }
}
