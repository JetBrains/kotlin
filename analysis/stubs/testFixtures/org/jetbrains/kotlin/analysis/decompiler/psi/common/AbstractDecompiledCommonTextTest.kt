/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.common

import org.jetbrains.kotlin.analysis.decompiler.psi.AbstractDecompiledTextTest
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleDecompiler
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleDecompilerDirectory
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

/**
 * Covers .knm (K2) and .kotlin_metadata (K1) files
 *
 * @see org.jetbrains.kotlin.analysis.stubs.common.AbstractCompiledCommonStubsTest
 */
abstract class AbstractDecompiledCommonTextTest : AbstractDecompiledTextTest(CommonPlatforms.defaultCommonPlatform) {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useAdditionalService<TestModuleDecompiler> { TestModuleDecompilerDirectory() }
    }
}
