/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.generators.featureinteraction

enum class TemplateValidationTransformerType {
    TRIM_BACKTICKS
}

val templateValidationTransformers = mapOf<TemplateValidationTransformerType, (String) -> String>(
    TemplateValidationTransformerType.TRIM_BACKTICKS to { element -> element.trim('`') }
)
