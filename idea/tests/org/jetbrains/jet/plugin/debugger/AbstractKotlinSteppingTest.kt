/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.debugger

import com.intellij.debugger.engine.SuspendContextImpl
import org.jetbrains.jet.plugin.debugger.KotlinSmartStepIntoHandler.KotlinMethodSmartStepTarget
import org.jetbrains.jet.plugin.debugger.KotlinSmartStepIntoHandler.KotlinBasicStepMethodFilter
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.debugger.actions.MethodSmartStepTarget
import com.intellij.debugger.engine.BasicStepMethodFilter
import org.jetbrains.jet.plugin.util.application.runReadAction
import org.jetbrains.jet.InTextDirectivesUtils.*
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import kotlin.properties.Delegates
import com.intellij.debugger.settings.DebuggerSettings

public abstract class AbstractKotlinSteppingTest : KotlinDebuggerTestBase() {
    private var oldSettings: DebuggerSettings by Delegates.notNull()
    private var oldIsFilterForStdlibAlreadyAdded: Boolean by Delegates.notNull()
    private var oldDisableKotlinInternalClasses: Boolean by Delegates.notNull()

    override fun initApplication() {
        super.initApplication()
        saveDefaultSettings()
    }

    override fun tearDown() {
        super.tearDown()
        restoreDefaultSettings()
    }

    protected fun doStepIntoTest(path: String) {
        val fileText = FileUtil.loadFile(File(path))

        configureSettings(fileText)

        createDebugProcess(path)
        val count = findStringWithPrefixes(fileText, "// STEP_INTO: ")?.toInt() ?: 1

        for (i in 1..count) {
            onBreakpoint { stepInto() }
        }

        finish()
    }

    protected fun doSmartStepIntoTest(path: String) {
        createDebugProcess(path)
        onBreakpoint { smartStepInto() }
        finish()
    }

    private fun configureSettings(fileText: String) {
        val kotlinSettings = KotlinDebuggerSettings.getInstance()
        kotlinSettings.DEBUG_IS_FILTER_FOR_STDLIB_ALREADY_ADDED = false
        kotlinSettings.DEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES = fileText.getValueForSetting("DISABLE_KOTLIN_INTERNAL_CLASSES", oldDisableKotlinInternalClasses)

        val debuggerSettings = DebuggerSettings.getInstance()!!
        debuggerSettings.SKIP_CONSTRUCTORS = fileText.getValueForSetting("SKIP_CONSTRUCTORS", oldSettings.SKIP_CONSTRUCTORS)
        debuggerSettings.SKIP_CLASSLOADERS = fileText.getValueForSetting("SKIP_CLASSLOADERS", oldSettings.SKIP_CLASSLOADERS)
        debuggerSettings.TRACING_FILTERS_ENABLED = fileText.getValueForSetting("TRACING_FILTERS_ENABLED", oldSettings.TRACING_FILTERS_ENABLED)
    }

    private fun String.getValueForSetting(name: String, defaultValue: Boolean): Boolean {
        return findStringWithPrefixes(this, "// $name: ")?.toBoolean() ?: defaultValue
    }

    private fun saveDefaultSettings() {
        oldIsFilterForStdlibAlreadyAdded = KotlinDebuggerSettings.getInstance().DEBUG_IS_FILTER_FOR_STDLIB_ALREADY_ADDED
        oldDisableKotlinInternalClasses = KotlinDebuggerSettings.getInstance().DEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES
        oldSettings = DebuggerSettings.getInstance()!!.clone()
    }

    private fun restoreDefaultSettings() {
        KotlinDebuggerSettings.getInstance().DEBUG_IS_FILTER_FOR_STDLIB_ALREADY_ADDED = oldIsFilterForStdlibAlreadyAdded
        KotlinDebuggerSettings.getInstance().DEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES = oldDisableKotlinInternalClasses

        val debuggerSettings = DebuggerSettings.getInstance()!!
        debuggerSettings.SKIP_CONSTRUCTORS = oldSettings.SKIP_CONSTRUCTORS
        debuggerSettings.SKIP_CLASSLOADERS = oldSettings.SKIP_CLASSLOADERS
        debuggerSettings.TRACING_FILTERS_ENABLED = oldSettings.TRACING_FILTERS_ENABLED
    }

    private fun SuspendContextImpl.smartStepInto() {
        this.smartStepInto(false)
    }

    private fun SuspendContextImpl.smartStepInto(ignoreFilters: Boolean) {
        createSmartStepIntoFilters().forEach {
            dp.getManagerThread()!!.schedule(dp.createStepIntoCommand(this, ignoreFilters, it))
        }
    }

    private fun createSmartStepIntoFilters(): List<BasicStepMethodFilter> {
        val breakpointManager = DebuggerManagerEx.getInstanceEx(getProject())?.getBreakpointManager()
        val breakpoint = breakpointManager?.getBreakpoints()?.first { it is LineBreakpoint }

        val line = (breakpoint as LineBreakpoint).getLineIndex()

        return runReadAction {
            val containingFile = breakpoint.getPsiFile()
            if (containingFile == null) throw AssertionError("Couldn't find file for breakpoint at the line $line")

            val position = MockSourcePosition(_file = containingFile, _line = line)

            val stepTargets = KotlinSmartStepIntoHandler().findSmartStepTargets(position)

            stepTargets.filterIsInstance(javaClass<MethodSmartStepTarget>()).map {
                stepTarget ->
                when (stepTarget) {
                    is KotlinMethodSmartStepTarget -> KotlinBasicStepMethodFilter(stepTarget as KotlinMethodSmartStepTarget)
                    else -> BasicStepMethodFilter(stepTarget.getMethod(), stepTarget.getCallingExpressionLines())
                }
            }
        }
    }
}
