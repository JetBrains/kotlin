/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import java.io.File
import kotlin.properties.Delegates

abstract class TestWithWorkingDir : TestCase() {
    protected var workingDir: File by Delegates.notNull()
        private set

    @Before
    public override fun setUp() {
        super.setUp()
        workingDir = FileUtil.createTempDirectory(this::class.java.simpleName, null, /* deleteOnExit = */ true)
    }

    @After
    public override fun tearDown() {
        workingDir.deleteRecursively()
        super.tearDown()
    }
}