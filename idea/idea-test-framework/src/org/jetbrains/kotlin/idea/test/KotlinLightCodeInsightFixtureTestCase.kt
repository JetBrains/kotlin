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
import com.intellij.openapi.module.Module
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.LoggedErrorProcessor
import org.apache.log4j.Logger
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.rethrow
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.reflect.full.findAnnotation

abstract class KotlinLightCodeInsightFixtureTestCase : KotlinLightCodeInsightFixtureTestCaseBase() {
    private var kotlinInternalModeOriginalValue = false

    private val exceptions = ArrayList<Throwable>()

    protected val module: Module get() = myFixture.module

    override fun setUp() {
        super.setUp()
        (StartupManager.getInstance(project) as StartupManagerImpl).runPostStartupActivities()
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory())

        kotlinInternalModeOriginalValue = KotlinInternalMode.enabled
        KotlinInternalMode.enabled = true

        project.getComponent(EditorTracker::class.java)?.projectOpened()

        invalidateLibraryCache(project)

        LoggedErrorProcessor.setNewInstance(object : LoggedErrorProcessor() {
            override fun processError(message: String?, t: Throwable?, details: Array<out String>?, logger: Logger) {
                exceptions.addIfNotNull(t)
                super.processError(message, t, details, logger)
            }
        })
    }

    override fun tearDown() {
        LoggedErrorProcessor.restoreDefaultProcessor()

        KotlinInternalMode.enabled = kotlinInternalModeOriginalValue
        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory())

        doKotlinTearDown(project) {
            super.tearDown()
        }

        if (exceptions.isNotEmpty()) {
            exceptions.forEach { it.printStackTrace() }
            throw AssertionError("Exceptions in other threads happened")
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor
            = getProjectDescriptorFromFileDirective()

    protected fun getProjectDescriptorFromTestName(): LightProjectDescriptor {
        val testName = StringUtil.toLowerCase(getTestName(false))

        if (testName.endsWith("runtime")) {
            return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
        }
        else if (testName.endsWith("stdlib")) {
            return ProjectDescriptorWithStdlibSources.INSTANCE
        }

        return KotlinLightProjectDescriptor.INSTANCE
    }

    protected fun getProjectDescriptorFromFileDirective(): LightProjectDescriptor {
        if (!isAllFilesPresentInTest()) {
            try {
                val fileText = FileUtil.loadFile(File(testDataPath, fileName()), true)

                val withLibraryDirective = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "WITH_LIBRARY:")
                if (!withLibraryDirective.isEmpty()) {
                    return JdkAndMockLibraryProjectDescriptor(PluginTestCaseBase.getTestDataPathBase() + "/" + withLibraryDirective.get(0), true)
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME_WITH_SOURCES")) {
                    return ProjectDescriptorWithStdlibSources.INSTANCE
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME_WITH_KOTLIN_TEST")) {
                    return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_WITH_KOTLIN_TEST
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME") ||
                         InTextDirectivesUtils.isDirectiveDefined(fileText, "WITH_RUNTIME")) {
                    return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "JS")) {
                    return KotlinStdJSProjectDescriptor
                }
            }
            catch (e: IOException) {
                throw rethrow(e)
            }
        }

        return KotlinLightProjectDescriptor.INSTANCE
    }

    protected fun isAllFilesPresentInTest(): Boolean = KotlinTestUtils.isAllFilesPresentTest(getTestName(false))

    protected open fun fileName(): String
            = KotlinTestUtils.getTestDataFileName(this::class.java, this.name) ?: (getTestName(false) + ".kt")

    protected fun performNotWriteEditorAction(actionId: String): Boolean {
        val dataContext = (myFixture.editor as EditorEx).dataContext

        val managerEx = ActionManagerEx.getInstanceEx()
        val action = managerEx.getAction(actionId)
        val event = AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, Presentation(), managerEx, 0)

        action.update(event)
        if (!event.presentation.isEnabled) {
            return false
        }

        managerEx.fireBeforeActionPerformed(action, dataContext, event)
        action.actionPerformed(event)

        managerEx.fireAfterActionPerformed(action, dataContext, event)
        return true
    }

    override fun getTestDataPath(): String {
        return this::class.findAnnotation<TestMetadata>()?.value ?: super.getTestDataPath()
    }
}
