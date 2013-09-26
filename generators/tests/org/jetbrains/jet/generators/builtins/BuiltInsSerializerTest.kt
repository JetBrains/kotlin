/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.generators.builtins

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.Function
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jet.JetTestUtils
import java.io.File
import java.util.Arrays
import java.util.HashSet
import java.util.regex.Pattern
import junit.framework.Assert

public open class BuiltInsSerializerTest() : UsefulTestCase() {
    public fun testBuiltIns() {
        val actual = JetTestUtils.tmpDir("builtins")
        BuiltInsSerializer.serializeToDir(actual, null)

        val expected = File(BuiltInsSerializer.DEST_DIR)

        val actualFiles = getAllFiles(actual)
        val expectedFiles = getAllFiles(expected)

        val actualNames = getFileNames(actualFiles)
        val expectedNames = getFileNames(expectedFiles)

        Assert.assertEquals("File name sets differ. Re-run BuiltInsSerializer", expectedNames, actualNames)
        for (actualFile in actualFiles) {
            if (actualFile.isDirectory()) continue

            val relativePath = FileUtil.getRelativePath(actual, actualFile)!!
            val expectedFile = File(expected, relativePath)

            val expectedBytes = FileUtil.loadFileBytes(expectedFile)
            val actualBytes = FileUtil.loadFileBytes(actualFile)
            Assert.assertTrue("File contents differ for $expectedFile and $actualFile. Re-run BuiltInsSerializer",
                              Arrays.equals(expectedBytes, actualBytes))
        }
        println("${actualFiles.size()} files checked")
    }

    private fun getAllFiles(actual: File): List<File> = FileUtil.findFilesByMask(Pattern.compile(".*"), actual)

    private fun getFileNames(actualFiles: List<File>): Set<String> = HashSet(ContainerUtil.map(actualFiles, {f -> f!!.getName()}))
}
