/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.analysis.api.annotations.renderAsSourceCode
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.classId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtAnnotationValue
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

internal fun PrettyPrinter.renderFe10Annotations(
    annotations: Annotations,
    isSingleLineAnnotations: Boolean,
    renderAnnotationWithShortNames: Boolean,
    analysisContext: Fe10AnalysisContext,
    predicate: (ClassId) -> Boolean = { true }
) {
    val separator = if (isSingleLineAnnotations) " " else "\n"
    for (annotation in annotations) {
        val annotationClass = annotation.annotationClass ?: continue
        val classId = annotationClass.classId
        if (classId != null && !predicate(classId)) {
            continue
        }

        if (annotationClass.fqNameSafe != StandardNames.FqNames.parameterName) {
            append('@')
            val rendered = if (renderAnnotationWithShortNames) annotation.fqName?.shortName()?.render() else annotation.fqName?.render()
            append(rendered ?: "ERROR")

            val valueArguments = annotation.allValueArguments.entries.sortedBy { it.key.asString() }
            printCollectionIfNotEmpty(valueArguments, separator = ", ", prefix = "(", postfix = ")") { (name, value) ->
                append(name.render())
                append(" = ")
                append(value.toKtAnnotationValue(analysisContext).renderAsSourceCode())
            }

            append(separator)
        }
    }
}