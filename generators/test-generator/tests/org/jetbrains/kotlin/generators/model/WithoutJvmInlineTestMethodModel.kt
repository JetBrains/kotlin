/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

class WithoutJvmInlineTestMethodModel(
    source: SimpleTestMethodModel,
    withAnnotation: Boolean?,
    withPostfix: Boolean,
) : TransformingTestMethodModel(
    source,
    transformer = "TransformersFunctions.get" +
            when (withAnnotation) {
                true -> "ReplaceOptionalJvmInlineAnnotationWithReal()"
                false -> "RemoveOptionalJvmInlineAnnotation()"
                null -> "ReplaceOptionalJvmInlineAnnotationWithUniversal()"
            }
) {
    override val name: String = source.name + if (withPostfix) "_valueClasses" else ""
}