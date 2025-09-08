/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractKotlinCompilerWithTargetBackendTest(
    override val targetBackend: TargetBackend
) : AbstractKotlinCompilerTest(), RunnerWithTargetBackendForTestGeneratorMarker {
    @TestInfrastructureInternals
    final override fun configureInternal(builder: TestConfigurationBuilder) {
        configure(builder)
        with(builder) {
            globalDefaults {
                val targetBackendFromMarker = this@AbstractKotlinCompilerWithTargetBackendTest.targetBackend
                if (targetBackend == null) {
                    targetBackend = this@AbstractKotlinCompilerWithTargetBackendTest.targetBackend
                } else {
                    require(targetBackend == targetBackendFromMarker) {
                        """Target backend in configuration specified to $targetBackend but in 
                          |AbstractKotlinCompilerWithTargetBackendTest parent it is set to $targetBackendFromMarker""".trimMargin()
                    }
                }
            }
        }
    }
}
