/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.argumentMapping
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val RETENTION_PARAMETER_NAME = Name.identifier("value")
private val TARGET_PARAMETER_NAME = Name.identifier("allowedTargets")

fun FirAnnotationCall.getRetention(session: FirSession): AnnotationRetention {
    val annotationClass = (this.annotationTypeRef.coneType as? ConeClassLikeType)?.lookupTag?.toSymbol(session)?.fir as? FirRegularClass
    return annotationClass?.getRetention() ?: AnnotationRetention.RUNTIME
}

fun FirRegularClass.getRetention(): AnnotationRetention {
    val retentionAnnotation = getRetentionAnnotation() ?: return AnnotationRetention.RUNTIME
    val argumentMapping = retentionAnnotation.argumentMapping ?: return AnnotationRetention.RUNTIME
    val retentionArgument = argumentMapping.keys.firstOrNull() as? FirQualifiedAccessExpression
        ?: return AnnotationRetention.RUNTIME
    if (argumentMapping[retentionArgument]?.name != RETENTION_PARAMETER_NAME) {
        return AnnotationRetention.RUNTIME
    }
    val retentionName = (retentionArgument.calleeReference as? FirResolvedNamedReference)?.name?.asString()
        ?: return AnnotationRetention.RUNTIME
    return AnnotationRetention.values().firstOrNull { it.name == retentionName } ?: AnnotationRetention.RUNTIME
}

private val defaultAnnotationTargets = listOf(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)

fun FirAnnotationCall.getAllowedAnnotationTargets(session: FirSession): List<AnnotationTarget> {
    val annotationClass = (this.annotationTypeRef.coneType as? ConeClassLikeType)?.lookupTag?.toSymbol(session)?.fir as? FirRegularClass
    return annotationClass?.getAllowedAnnotationTargets() ?: defaultAnnotationTargets
}

fun FirRegularClass.getAllowedAnnotationTargets(): List<AnnotationTarget> {
    val targetAnnotation = getTargetAnnotation() ?: return defaultAnnotationTargets
    val argumentMapping = targetAnnotation.argumentMapping ?: return defaultAnnotationTargets
    val targetArgument = argumentMapping.keys.firstOrNull() as? FirVarargArgumentsExpression
        ?: return defaultAnnotationTargets
    if (argumentMapping[targetArgument]?.name != TARGET_PARAMETER_NAME) {
        return defaultAnnotationTargets
    }
    return targetArgument.arguments.mapNotNull { argument ->
        val targetExpression = argument as? FirQualifiedAccessExpression
        val targetName = (targetExpression?.calleeReference as? FirResolvedNamedReference)?.name?.asString() ?: return@mapNotNull null
        AnnotationTarget.values().firstOrNull { target -> target.name == targetName }
    }
}

fun FirAnnotatedDeclaration.getRetentionAnnotation(): FirAnnotationCall? {
    return getAnnotationByFqName(StandardNames.FqNames.retention)
}

fun FirAnnotatedDeclaration.getTargetAnnotation(): FirAnnotationCall? {
    return getAnnotationByFqName(StandardNames.FqNames.target)
}

fun FirAnnotatedDeclaration.getAnnotationByFqName(fqName: FqName): FirAnnotationCall? {
    return annotations.find {
        (it.annotationTypeRef.coneType as? ConeClassLikeType)?.lookupTag?.classId?.asSingleFqName() == fqName
    }
}

