/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.openapi.util.io.FileUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.File
import kotlin.properties.Delegates

abstract class TestWithWorkingDir {
    protected var workingDir: File by Delegates.notNull()
        private set

    @BeforeEach
    open fun setUp() {
        workingDir = FileUtil.createTempDirectory(this::class.java.simpleName, null, /* deleteOnExit = */ true)
    }

    @AfterEach
    open fun tearDown() {
        workingDir.deleteRecursively()
    }
}
