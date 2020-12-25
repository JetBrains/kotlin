/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractKotlinCompilerWithTargetBackendTest(
    override val targetBackend: TargetBackend
) : AbstractKotlinCompilerTest(), RunnerWithTargetBackendForTestGeneratorMarker {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            globalDefaults {
                targetBackend = this@AbstractKotlinCompilerWithTargetBackendTest.targetBackend
            }
        }
    }
}
