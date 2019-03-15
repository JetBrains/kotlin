/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractKotlinSteppingTest : KotlinDebuggerTestBase() {
    protected fun doStepIntoTest(path: String) {
        doTest(path, "STEP_INTO")
    }

    protected fun doStepOutTest(path: String) {
        doTest(path, "STEP_OUT")
    }

    protected fun doStepOverTest(path: String) {
        doTest(path, "STEP_OVER")
    }

    protected fun doStepOverForceTest(path: String) {
        doTest(path, "STEP_OVER_FORCE")
    }

    protected fun doSmartStepIntoTest(path: String) {
        doTest(path, "SMART_STEP_INTO")
    }

    protected fun doCustomTest(path: String) {
        val fileText = FileUtil.loadFile(File(path))
        configureSettings(fileText)
        createAdditionalBreakpoints(fileText)
        createDebugProcess(path)

        doStepping(path)

        finish()
    }

    private fun doTest(path: String, command: String) {
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
