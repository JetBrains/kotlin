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

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.intellij.openapi.vfs.VfsUtil

import java.io.File

open class KotlinJdkAndLibraryProjectDescriptor(private val libraryFiles: List<File>) : KotlinLightProjectDescriptor() {

    constructor(libraryFile: File) : this(listOf(libraryFile))

    init {
        for (libraryFile in libraryFiles) {
            assert(libraryFile.exists()) { "Library file doesn't exist: " + libraryFile.absolutePath }
        }
    }

    override fun getModuleType(): ModuleType<*> = StdModuleTypes.JAVA

    override fun getSdk(): Sdk? = PluginTestCaseBase.mockJdk()

    override fun configureModule(module: Module, model: ModifiableRootModel) {
        val editor = NewLibraryEditor()
        editor.name = LIBRARY_NAME
        for (libraryFile in libraryFiles) {
            editor.addRoot(VfsUtil.getUrlForLibraryRoot(libraryFile), OrderRootType.CLASSES)
        }

        ConfigLibraryUtil.addLibrary(editor, model)
    }

    companion object {
        val LIBRARY_NAME = "myLibrary"
    }
}
