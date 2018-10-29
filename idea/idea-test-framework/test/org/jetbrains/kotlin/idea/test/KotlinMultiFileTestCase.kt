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

import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.MultiFileTestCase
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.KotlinTestUtils

abstract class KotlinMultiFileTestCase : MultiFileTestCase() {
    protected var isMultiModule = false

    override fun setUp() {
        super.setUp()
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory())

        runWriteAction {
            PluginTestCaseBase.addJdk(testRootDisposable, PluginTestCaseBase::mockJdk6)
            ProjectRootManager.getInstance(project).projectSdk = PluginTestCaseBase.mockJdk6()
        }
    }

    protected fun getTestDirName(lowercaseFirstLetter : Boolean) : String {
        val testName = getTestName(lowercaseFirstLetter)
        val endIndex = testName.lastIndexOf('_')
        if (endIndex < 0) return testName
        return testName.substring(0, endIndex).replace('_', '/')
    }

    protected fun doTestCommittingDocuments(action : (VirtualFile, VirtualFile?) -> Unit) {
        super.doTest({ rootDir, rootAfter ->
                         action(rootDir, rootAfter)

                         PsiDocumentManager.getInstance(project!!).commitAllDocuments()
                         FileDocumentManager.getInstance().saveAllDocuments()
                     }, getTestDirName(true))
    }

    override fun prepareProject(rootDir: VirtualFile) {
        if (isMultiModule) {
            val model = ModuleManager.getInstance(project).modifiableModel

            VfsUtilCore.visitChildrenRecursively(
                    rootDir,
                    object : VirtualFileVisitor<Any>() {
                        override fun visitFile(file: VirtualFile): Boolean {
                            if (!file.isDirectory && file.name.endsWith(ModuleFileType.DOT_DEFAULT_EXTENSION)) {
                                model.loadModule(file.path)
                                return false
                            }

                            return true
                        }
                    }
            )

            runWriteAction { model.commit() }
        }
        else {
            PsiTestUtil.addSourceContentToRoots(myModule, rootDir)
        }
    }

    override fun tearDown() {
        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory())
        super.tearDown()
    }
}