/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.renderer

import org.jetbrains.kotlin.analysis.api.fir.annotations.mapAnnotationParameters
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirCompileTimeConstantEvaluator
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.annotations.KtUnsupportedAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.renderAsSourceCode
import org.jetbrains.kotlin.analysis.api.components.KtConstantEvaluationMode
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import kotlin.text.Appendable

internal fun Appendable.renderAnnotations(
    coneTypeIdeRenderer: ConeTypeIdeRenderer,
    annotations: List<FirAnnotation>,
    session: FirSession,
    isSingleLineAnnotations: Boolean,
) {
    val separator = if (isSingleLineAnnotations) " " else "\n"
    for (annotation in annotations) {
        if (!annotation.isParameterName()) {
            append(renderAnnotation(annotation, coneTypeIdeRenderer, session))
            append(separator)
        }
    }
}


private fun FirAnnotation.isParameterName(): Boolean {
    return toAnnotationClassId()?.asSingleFqName() == StandardNames.FqNames.parameterName
}

private fun renderAnnotation(annotation: FirAnnotation, coneTypeIdeRenderer: ConeTypeIdeRenderer, session: FirSession): String {
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

private fun renderAndSortAnnotationArguments(descriptor: FirAnnotation, session: FirSession): List<String> {
    val argumentList = mapAnnotationParameters(descriptor, session).entries.map { (name, value) ->
        "$name = ${renderConstant(value, session)}"
    }
    return argumentList.sorted()
}

private fun renderConstant(value: FirExpression, useSiteSession: FirSession): String {
    val evaluated = FirCompileTimeConstantEvaluator.evaluate(value, KtConstantEvaluationMode.CONSTANT_EXPRESSION_EVALUATION)
    val constantValue = FirAnnotationValueConverter.toConstantValue(evaluated ?: value, useSiteSession)
        ?: KtUnsupportedAnnotationValue

    return constantValue.renderAsSourceCode()
}
