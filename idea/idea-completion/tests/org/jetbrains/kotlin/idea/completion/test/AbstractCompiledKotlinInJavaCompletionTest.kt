/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms

abstract class AbstractCompiledKotlinInJavaCompletionTest : KotlinFixtureCompletionBaseTestCase() {
    override fun getPlatform() = DefaultBuiltInPlatforms.jvmPlatform

    override fun getProjectDescriptor() =
        SdkAndMockLibraryProjectDescriptor(COMPLETION_TEST_DATA_BASE_PATH + "/injava/mockLib", false)
    override fun defaultCompletionType() = CompletionType.BASIC
}
