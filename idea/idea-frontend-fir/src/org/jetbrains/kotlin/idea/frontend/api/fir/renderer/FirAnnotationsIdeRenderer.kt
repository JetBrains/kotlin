/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.renderer

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.mapAnnotationParameters

internal fun StringBuilder.renderAnnotations(
    coneTypeIdeRenderer: ConeTypeIdeRenderer,
    annotations: List<FirAnnotationCall>,
    session: FirSession
) {
    for (annotation in annotations) {
        if (!annotation.isParameterName()) {
            append(renderAnnotation(annotation, coneTypeIdeRenderer, session))
            append(" ")
        }
    }
}

private fun FirAnnotationCall.isParameterName(): Boolean {
    return toAnnotationClassId().asSingleFqName() == StandardNames.FqNames.parameterName
}

private fun renderAnnotation(annotation: FirAnnotationCall, coneTypeIdeRenderer: ConeTypeIdeRenderer, session: FirSession): String {
    return buildString {
        append('@')
        val resolvedTypeRef = annotation.typeRef as? FirResolvedTypeRef
        check(resolvedTypeRef != null)
        append(coneTypeIdeRenderer.renderType(resolvedTypeRef.type))

        val arguments = renderAndSortAnnotationArguments(annotation, session)
        if (arguments.isNotEmpty()) {
            arguments.joinTo(this, ", ", "(", ")")
        }
    }
}

private fun renderAndSortAnnotationArguments(descriptor: FirAnnotationCall, session: FirSession): List<String> {
    val argumentList = mapAnnotationParameters(descriptor, session).entries.map { (name, value) ->
        "$name = ${renderConstant(value)}"
    }
    return argumentList.sorted()
}

private fun renderConstant(value: FirExpression): String {
    return when (value) {
        is FirConstExpression<*> -> value.toString()
        else -> "NOT_CONST_EXPRESSION"
    }
}