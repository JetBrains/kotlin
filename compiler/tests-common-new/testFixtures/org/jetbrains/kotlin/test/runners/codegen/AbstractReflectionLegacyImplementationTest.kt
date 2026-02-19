/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.REQUIRES_SEPARATE_PROCESS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_LEGACY_REFLECTION_IMPLEMENTATION

open class AbstractReflectionLegacyImplementationTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +USE_LEGACY_REFLECTION_IMPLEMENTATION
                +REQUIRES_SEPARATE_PROCESS
            }
        }
    }
}
