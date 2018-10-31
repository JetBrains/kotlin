/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.generators.templates

import org.jetbrains.kotlin.spec.tasks.generateTests

fun generationSpecTestDataConfigGroup(regenerateTests: Boolean = false, body: () -> Unit) {
    body()
    if (regenerateTests) generateTests()
}

fun generationLinkedSpecTestDataConfig(body: GenerationLinkedSpecTestDataConfig.() -> Unit) =
    GenerationLinkedSpecTestDataConfig().also {
        body(it)
        FeatureInteractionTestDataGenerator(it).generate()
    }

fun generationNotLinkedSpecTestDataConfig(body: GenerationNotLinkedSpecTestDataConfig.() -> Unit) =
    GenerationNotLinkedSpecTestDataConfig().also {
        body(it)
        FeatureInteractionTestDataGenerator(it).generate()
    }
