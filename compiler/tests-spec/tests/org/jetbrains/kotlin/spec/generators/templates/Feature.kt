/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.generators.templates

enum class Feature(val config: FeatureTemplatesConfig) {
    IDENTIFIERS(
        FeatureTemplatesConfig(
            FeatureTemplatesType.AS_FILE,
            templatesPath = "identifiers"
        )
    ),
    BOOLEAN_LITERALS(
        FeatureTemplatesConfig(
            FeatureTemplatesType.AS_STRING,
            templates = mapOf("true" to "true", "false" to "false")
        )
    ),
    BOOLEAN_LITERALS_IN_BACKTICKS(
        FeatureTemplatesConfig(
            FeatureTemplatesType.AS_STRING,
            templates = mapOf("trueWithBacktick" to "`true`", "falseWithBacktick" to "`false`"),
            validationTransformer = TemplateValidationTransformerType.TRIM_BACKTICKS
        )
    )
}
