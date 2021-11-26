/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.classId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtConstantValueRenderer
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

internal fun KtFe10RendererConsumer.renderFe10Annotations(annotations: Annotations, predicate: (ClassId) -> Boolean = { true }) {
    for (annotation in annotations) {
        val annotationClass = annotation.annotationClass ?: continue
        val classId = annotationClass.classId
        if (classId != null && !predicate(classId)) {
            continue
        }

        if (annotationClass.fqNameSafe != StandardNames.FqNames.parameterName) {
            append('@')
            append(annotation.fqName?.shortName()?.asString() ?: "ERROR")

            val valueArguments = annotation.allValueArguments.entries.sortedBy { it.key.asString() }
            renderList(valueArguments, separator = ", ", prefix = "(", postfix = ")", renderWhenEmpty = false) { (name, value) ->
                append(name.render())
                append(" = ")
                append(KtConstantValueRenderer.render(value.toKtConstantValue()))
            }

            append(' ')
        }
    }
}