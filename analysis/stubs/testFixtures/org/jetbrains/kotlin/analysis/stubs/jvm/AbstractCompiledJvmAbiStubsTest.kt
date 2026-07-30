/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs.jvm

import org.jetbrains.kotlin.analysis.stubs.AbstractCompiledStubsTest
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.JvmAbiTestModuleCompiler
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleCompiler
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractCompiledJvmAbiStubsTest : AbstractCompiledStubsTest(JvmPlatforms.defaultJvmPlatform) {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useAdditionalService<TestModuleCompiler> { JvmAbiTestModuleCompiler }
    }
}
