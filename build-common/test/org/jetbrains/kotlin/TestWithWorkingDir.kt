/*
 * Copyright 2010-2017 JetBrains s.r.o.
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