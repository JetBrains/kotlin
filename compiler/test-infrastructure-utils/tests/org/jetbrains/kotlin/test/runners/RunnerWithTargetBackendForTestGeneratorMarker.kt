/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.TargetBackend

/**
 * If your test runner has specific target backend then you can add this interface
 *   to your runner and after that you should not declare targetBackend
 *   parameter in DSL in addition to defining it in test runner
 *
 * Please make sure that all abstract runners which are used in test generator
 *   have `open` modality, not `abstract`. This is required because test generator
 *   instantiates runner to get value of targetBackend and generate tests properly
 */
interface RunnerWithTargetBackendForTestGeneratorMarker {
    val targetBackend: TargetBackend
}
