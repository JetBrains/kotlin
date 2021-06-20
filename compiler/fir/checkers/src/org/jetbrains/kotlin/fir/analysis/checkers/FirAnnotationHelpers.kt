/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
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
    val retentionArgument = retentionAnnotation.findArgumentByName(RETENTION_PARAMETER_NAME) as? FirQualifiedAccessExpression
        ?: return AnnotationRetention.RUNTIME
    val retentionName = (retentionArgument.calleeReference as? FirResolvedNamedReference)?.name?.asString()
        ?: return AnnotationRetention.RUNTIME
    return AnnotationRetention.values().firstOrNull { it.name == retentionName } ?: AnnotationRetention.RUNTIME
}

private val defaultAnnotationTargets = KotlinTarget.DEFAULT_TARGET_SET

fun FirAnnotationCall.getAllowedAnnotationTargets(session: FirSession): Set<KotlinTarget> {
    if (annotationTypeRef is FirErrorTypeRef) return KotlinTarget.values().toSet()
    val annotationClass = (this.annotationTypeRef.coneType as? ConeClassLikeType)
        ?.fullyExpandedType(session)?.lookupTag?.toSymbol(session)?.fir as? FirRegularClass
    return annotationClass?.getAllowedAnnotationTargets() ?: defaultAnnotationTargets
}

fun FirRegularClass.getAllowedAnnotationTargets(): Set<KotlinTarget> {
    val targetAnnotation = getTargetAnnotation() ?: return defaultAnnotationTargets
    if (targetAnnotation.argumentList.arguments.isEmpty()) return emptySet()
    val arguments = targetAnnotation.findArgumentByName(TARGET_PARAMETER_NAME)?.unfoldArrayOrVararg().orEmpty()

    return arguments.mapNotNullTo(mutableSetOf()) { argument ->
        val targetExpression = argument as? FirQualifiedAccessExpression
        val targetName = (targetExpression?.calleeReference as? FirResolvedNamedReference)?.name?.asString() ?: return@mapNotNullTo null
        KotlinTarget.values().firstOrNull { target -> target.name == targetName }
    }
}

fun FirAnnotatedDeclaration<*>.getRetentionAnnotation(): FirAnnotationCall? {
    return getAnnotationByFqName(StandardNames.FqNames.retention)
}

fun FirAnnotatedDeclaration<*>.getTargetAnnotation(): FirAnnotationCall? {
    return getAnnotationByFqName(StandardNames.FqNames.target)
}

fun FirAnnotationContainer.getAnnotationByFqName(fqName: FqName): FirAnnotationCall? {
    return annotations.find {
        (it.annotationTypeRef.coneType as? ConeClassLikeType)?.lookupTag?.classId?.asSingleFqName() == fqName
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

fun FirExpression.extractClassesFromArgument(): List<FirRegularClassSymbol> {
    return unfoldArrayOrVararg().mapNotNull {
        if (it !is FirGetClassCall) return@mapNotNull null
        val qualifier = it.argument as? FirResolvedQualifier ?: return@mapNotNull null
        qualifier.symbol as? FirRegularClassSymbol
    }
}

private fun FirExpression.unfoldArrayOrVararg(): List<FirExpression> {
    return when (this) {
        is FirVarargArgumentsExpression -> arguments
        is FirArrayOfCall -> arguments
        else -> return emptyList()
    }
}

