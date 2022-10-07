/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.resolved
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildVarargArgumentsExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal fun mapAnnotationParameters(annotation: FirAnnotation, session: FirSession): Map<Name, FirExpression> {
    // TODO: Alas, argument mapping for annotations on [FirValueParameter] is not properly built, even after BODY_RESOLVE ensured.
    //  Once fixed, manual building of argument mapping below is redundant. I.e., this util can be as simple as:
    //    return if (annotation.resolved)
    //      annotation.argumentMapping.mapping.mapKeys { (name, _) -> name }
    //    else
    //      emptyMap()
    if (annotation.resolved) return annotation.argumentMapping.mapping.mapKeys { (name, _) -> name }
    if (annotation !is FirAnnotationCall) return emptyMap()
    val annotationCone = (annotation.annotationTypeRef.coneType as? ConeClassLikeType)?.fullyExpandedType(session)
        ?: return emptyMap()

    val annotationPrimaryCtor = (annotationCone.lookupTag.toSymbol(session)?.fir as? FirRegularClass)?.primaryConstructorIfAny(session)?.fir
    val valueParameters = annotationPrimaryCtor?.valueParameters
    val (varargValueParameters, nonVarargValueParameters) = valueParameters?.partition { it.isVararg }
        ?: (emptyList<FirValueParameter>() to emptyList())
    val annotationCtorParameterNames = nonVarargValueParameters.map { it.name }

    val resultMap = mutableMapOf<Name, FirExpression>()

    val namesSequence = annotationCtorParameterNames.asSequence().iterator()

    for (argument in annotation.argumentList.arguments.filterIsInstance<FirNamedArgumentExpression>()) {
        resultMap[argument.name] = argument.expression
    }

    val argumentSequence = annotation.argumentList.arguments.asSequence().iterator()

    while (namesSequence.hasNext()) {
        val name = namesSequence.next()
        if (name in resultMap) continue
        while (argumentSequence.hasNext()) {
            val argument = argumentSequence.next()
            if (argument is FirNamedArgumentExpression) continue

            resultMap[name] = argument
            break
        }
    }

    if (varargValueParameters.isNotEmpty() && argumentSequence.hasNext()) {
        val varargValueParameter = varargValueParameters.single()
        val arguments = buildList {
            while (argumentSequence.hasNext()) {
                val argument = argumentSequence.next()
                if (argument is FirNamedArgumentExpression) continue
                add(argument)
            }
        }
        if (arguments.isNotEmpty()) {
            val exp = buildVarargArgumentsExpression {
                this.arguments.addAll(arguments)
                val firstArgument = arguments.first()
                firstArgument.source?.let { source = it.fakeElement(KtFakeSourceElementKind.VarargArgument) }
                (varargValueParameter.returnTypeRef as? FirResolvedTypeRef)?.coneType?.varargElementType()?.let {
                    varargElementType = buildResolvedTypeRef {
                        source = varargValueParameter.returnTypeRef.source
                        type = it
                    }
                } ?: firstArgument.typeRef.let {
                    varargElementType = it
                }
            }
            resultMap[varargValueParameter.name] = exp
        }
    }

    return resultMap
}
