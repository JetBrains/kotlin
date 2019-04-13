/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.application.PathManager.PROPERTY_CONFIG_PATH
import com.intellij.openapi.application.PathManager.PROPERTY_SYSTEM_PATH
import com.intellij.openapi.util.io.FileUtil
import java.io.File

// It's important that this is not created per test, but rather per process.
object IdeaSystemPropertiesForParallelRunConfigurator {
    private val GRADLE_WORKER = System.getProperty("org.gradle.test.worker") ?: ""
    //TODO: try to remove folder on jvm shutdown (there are some flashing test with deleteOnExit = true)
    private val PROCESS_TMP_ROOT_FOLDER =
        FileUtil.createTempDirectory(File(System.getProperty("java.io.tmpdir")), "testRoot", GRADLE_WORKER, false).path
    private val IDEA_SYSTEM = FileUtil.createTempDirectory(File(PROCESS_TMP_ROOT_FOLDER), "idea-system", "", false).path
    private val IDEA_CONFIG = FileUtil.createTempDirectory(File(PROCESS_TMP_ROOT_FOLDER), "idea-config", "", false).path

    init {
        // UsefulTestCase temp dir construction could cause folder clash on parallel test execution:
        //  myTempDir = new File(ORIGINAL_TEMP_DIR, TEMP_DIR_MARKER + testName).getPath();
        // So we need to substitute "java.io.tmpdir" system property to avoid such clashing across different processes.
        // IDEA PR: https://github.com/JetBrains/intellij-community/pull/1120
        System.setProperty("java.io.tmpdir", PROCESS_TMP_ROOT_FOLDER)
        System.setProperty(PROPERTY_SYSTEM_PATH, IDEA_SYSTEM)
        System.setProperty(PROPERTY_CONFIG_PATH, IDEA_CONFIG)
    }

    @JvmStatic
    fun setProperties() {
        //TODO: maybe add check for actual folders
    }
}