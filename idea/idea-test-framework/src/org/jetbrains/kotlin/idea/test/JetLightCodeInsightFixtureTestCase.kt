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

package org.jetbrains.kotlin.idea.test

import com.intellij.codeInsight.daemon.impl.EditorTracker
import com.intellij.ide.startup.impl.StartupManagerImpl
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.utils.rethrow
import java.io.File
import java.io.IOException

public abstract class JetLightCodeInsightFixtureTestCase : LightCodeInsightFixtureTestCase() {
    private var kotlinInternalModeOriginalValue = false

    override fun setUp() {
        super.setUp()
        (StartupManager.getInstance(getProject()) as StartupManagerImpl).runPostStartupActivities()
        VfsRootAccess.allowRootAccess(JetTestUtils.getHomeDirectory())

        kotlinInternalModeOriginalValue = KotlinInternalMode.enabled
        KotlinInternalMode.enabled = true

        getProject().getComponent(javaClass<EditorTracker>())?.projectOpened()
    }

    override fun tearDown() {
        KotlinInternalMode.enabled = kotlinInternalModeOriginalValue
        VfsRootAccess.disallowRootAccess(JetTestUtils.getHomeDirectory())

        unInvalidateBuiltins(getProject()) {
            super.tearDown()
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor
            = getProjectDescriptorFromFileDirective()

    protected fun getProjectDescriptorFromTestName(): LightProjectDescriptor {
        val testName = StringUtil.toLowerCase(getTestName(false))

        if (testName.endsWith("runtime")) {
            return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
        }
        else if (testName.endsWith("stdlib")) {
            return ProjectDescriptorWithStdlibSources.INSTANCE
        }

        return JetLightProjectDescriptor.INSTANCE
    }

    protected fun getProjectDescriptorFromFileDirective(): LightProjectDescriptor {
        if (!isAllFilesPresentInTest()) {
            try {
                val fileText = FileUtil.loadFile(File(getTestDataPath(), fileName()), true)

                val withLibraryDirective = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "WITH_LIBRARY:")
                if (!withLibraryDirective.isEmpty()) {
                    return JdkAndMockLibraryProjectDescriptor(PluginTestCaseBase.getTestDataPathBase() + "/" + withLibraryDirective.get(0), true)
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME_WITH_SOURCES")) {
                    return ProjectDescriptorWithStdlibSources.INSTANCE
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME")) {
                    return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "JS")) {
                    return KotlinStdJSProjectDescriptor.instance
                }
            }
            catch (e: IOException) {
                throw rethrow(e)
            }
        }

        return JetLightProjectDescriptor.INSTANCE
    }

    protected fun isAllFilesPresentInTest(): Boolean
            = getTestName(false).startsWith("AllFilesPresentIn")

    protected open fun fileName(): String
            = getTestName(false) + ".kt"

    protected fun performNotWriteEditorAction(actionId: String): Boolean {
        val dataContext = (myFixture.getEditor() as EditorEx).getDataContext()

        val managerEx = ActionManagerEx.getInstanceEx()
        val action = managerEx.getAction(actionId)
        val event = AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, Presentation(), managerEx, 0)

        action.update(event)
        if (!event.getPresentation().isEnabled()) {
            return false
        }

        managerEx.fireBeforeActionPerformed(action, dataContext, event)
        action.actionPerformed(event)

        managerEx.fireAfterActionPerformed(action, dataContext, event)
        return true
    }
}
