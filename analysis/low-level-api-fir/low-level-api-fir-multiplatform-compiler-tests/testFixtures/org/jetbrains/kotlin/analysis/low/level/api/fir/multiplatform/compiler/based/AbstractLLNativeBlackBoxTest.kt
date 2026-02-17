/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.multiplatform.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLBlackBoxTest
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator

abstract class AbstractLLNativeBlackBoxTest : AbstractLLBlackBoxTest(
    NativePlatforms.unspecifiedNativePlatform
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForNativeBlackBoxTests()
    }
}

internal fun TestConfigurationBuilder.configureForNativeBlackBoxTests() {
    useConfigurators(::NativeEnvironmentConfigurator)
}