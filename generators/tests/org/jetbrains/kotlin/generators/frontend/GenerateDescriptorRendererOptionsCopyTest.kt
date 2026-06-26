/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.frontend

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.generators.frontend.GenerateDescriptorRendererOptionsCopy.generate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GenerateDescriptorRendererOptionsCopyTest {
    @Test
    fun testGeneratedDataIsUpToDate() {
        val text = generate()
        Assertions.assertEquals(
            StringUtil.convertLineSeparators(text),
            FileUtil.loadFile(GenerateDescriptorRendererOptionsCopy.DEST_FILE, true),
            "Contents differ. Regenerate " + GenerateDescriptorRendererOptionsCopy::class.java.getName()
        )
    }
}
