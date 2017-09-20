/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractKotlinSteppingTest : KotlinDebuggerTestBase() {
    protected fun doStepIntoTest(path: String) {
        assert(false) // TODO: Fix this test, otherwise it fails and hangs for 100 sec in AS 3.0
        doTest(path, "STEP_INTO")
    }

    protected fun doStepOutTest(path: String) {
        assert(false) // TODO: Fix this test, otherwise it fails and hangs for 100 sec in AS 3.0
        doTest(path, "STEP_OUT")
    }

    protected fun doStepOverTest(path: String) {
        assert(false) // TODO: Fix this test, otherwise it fails and hangs for 100 sec in AS 3.0
        doTest(path, "STEP_OVER")
    }

    protected fun doStepOverForceTest(path: String) {
        assert(false) // TODO: Fix this test, otherwise it fails and hangs for 100 sec in AS 3.0
        doTest(path, "STEP_OVER_FORCE")
    }

    protected fun doSmartStepIntoTest(path: String) {
        assert(false) // TODO: Fix this test, otherwise it fails and hangs for 100 sec in AS 3.0
        doTest(path, "SMART_STEP_INTO")
    }

    protected fun doCustomTest(path: String) {
        assert(false) // TODO: Fix this test, otherwise it fails and hangs for 100 sec in AS 3.0
        val fileText = FileUtil.loadFile(File(path))
        configureSettings(fileText)
        createAdditionalBreakpoints(fileText)
        createDebugProcess(path)

        doStepping(path)

        finish()
    }

    private fun doTest(path: String, command: String) {
        assert(false) // TODO: Fix this test, otherwise it fails and hangs for 100 sec in AS 3.0
        val fileText = FileUtil.loadFile(File(path))

        configureSettings(fileText)
        createAdditionalBreakpoints(fileText)
        createDebugProcess(path)

        val prefix = "// $command: "
        val count = InTextDirectivesUtils.getPrefixedInt(fileText, prefix) ?: "1"
        processSteppingInstruction("$prefix$count")

        finish()
    }
}
