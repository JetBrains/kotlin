/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.resolved
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId

internal fun mapAnnotationParameters(annotation: FirAnnotation, session: FirSession): Map<String, FirExpression> {
    if (annotation.resolved) return annotation.argumentMapping.mapping.mapKeys { (name, _) -> name.identifier }
    if (annotation !is FirAnnotationCall) return emptyMap()
    val annotationCone = annotation.annotationTypeRef.coneType as? ConeClassLikeType ?: return emptyMap()

    val annotationPrimaryCtor = (annotationCone.lookupTag.toSymbol(session)?.fir as? FirRegularClass)?.primaryConstructorIfAny(session)?.fir
    val annotationCtorParameterNames = annotationPrimaryCtor?.valueParameters?.map { it.name }

    val resultMap = mutableMapOf<String, FirExpression>()

    val namesSequence = annotationCtorParameterNames?.asSequence()?.iterator()

    for (argument in annotation.argumentList.arguments.filterIsInstance<FirNamedArgumentExpression>()) {
        resultMap[argument.name.asString()] = argument.expression
    }

    if (namesSequence != null) {
        for (argument in annotation.argumentList.arguments) {
            if (argument is FirNamedArgumentExpression) continue

            while (namesSequence.hasNext()) {
                val name = namesSequence.next().asString()
                if (!resultMap.contains(name)) {
                    resultMap[name] = argument
                    break
                }
            }
        }
    }

    return resultMap
}

internal fun FirAnnotation.fullyExpandedClassId(useSiteSession: FirSession): ClassId? =
    coneClassLikeType?.fullyExpandedType(useSiteSession)?.classId
