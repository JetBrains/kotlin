/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.frontend

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.generators.frontend.GenerateDescriptorRendererOptionsCopy.generate
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase

class GenerateDescriptorRendererOptionsCopyTest : KtUsefulTestCase() {
    fun testGeneratedDataIsUpToDate() {
        val text = generate()
        TestCase.assertEquals(
            "Contents differ. Regenerate " + GenerateDescriptorRendererOptionsCopy::class.java.getName(),
            StringUtil.convertLineSeparators(text),
            FileUtil.loadFile(GenerateDescriptorRendererOptionsCopy.DEST_FILE, true),
        )
    }
}
