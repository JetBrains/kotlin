/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.util.io.FileUtil
import org.junit.Assert
import java.io.File
import java.nio.file.Path

abstract class AbstractConfigureKotlinInTempDirTest : AbstractConfigureKotlinTest() {
    override fun getProjectDirOrFile(): Path {
        val tempDir = FileUtil.generateRandomTemporaryPath()
        FileUtil.createTempDirectory("temp", null)
        myFilesToDelete.add(tempDir)

        FileUtil.copyDir(File(projectRoot), tempDir)

        val projectRoot = tempDir.path

        val projectFilePath = projectRoot + "/projectFile.ipr"
        if (!File(projectFilePath).exists()) {
            val dotIdeaPath = projectRoot + "/.idea"
            Assert.assertTrue("Project file or '.idea' dir should exists in " + projectRoot, File(dotIdeaPath).exists())
            return File(projectRoot).toPath()
        }

        return File(projectFilePath).toPath()
    }
}