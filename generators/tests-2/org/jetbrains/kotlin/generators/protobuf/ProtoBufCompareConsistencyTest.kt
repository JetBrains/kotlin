/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.protobuf

import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class ProtoBufCompareConsistencyTest : TestCase() {
    fun testAlreadyGenerated() {
        val testDir = KtTestUtil.tmpDir("testDirectory")
        val newFile = File(testDir, "ProtoCompareGenerated.kt")
        GenerateProtoBufCompare.generate(newFile, forbidGenerationOnTeamcity = false)

        KotlinTestUtils.assertEqualsToFile(newFile, FileUtil.loadFile(GenerateProtoBufCompare.DEST_FILE))
    }
}
