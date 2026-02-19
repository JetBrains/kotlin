/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractKotlinCompilerWithTargetBackendTest(
    val targetBackend: TargetBackend,
) : AbstractKotlinCompilerTest() {
    @TestInfrastructureInternals
    final override fun configureInternal(builder: TestConfigurationBuilder) {
        val myTargetBackend = targetBackend
        configure(builder)
        with(builder) {
            globalDefaults {
                if (targetBackend == null) {
                    targetBackend = myTargetBackend
                } else {
                    require(targetBackend == myTargetBackend) {
                        """Target backend in configuration specified to $targetBackend but in 
                          |AbstractKotlinCompilerWithTargetBackendTest parent it is set to $myTargetBackend""".trimMargin()
                    }
                }
            }
        }
    }
}
