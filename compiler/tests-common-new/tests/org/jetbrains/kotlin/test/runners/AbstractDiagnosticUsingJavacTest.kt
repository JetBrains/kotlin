/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.handlers.DiagnosticTestWithJavacSkipConfigurator

abstract class AbstractDiagnosticUsingJavacTest : AbstractDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.apply {
            defaultDirectives {
                +JvmEnvironmentConfigurationDirectives.USE_JAVAC
            }
            useMetaTestConfigurators(::DiagnosticTestWithJavacSkipConfigurator)
        }
    }
}
