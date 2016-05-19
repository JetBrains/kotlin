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

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.MockLibraryUtil
import com.intellij.openapi.roots.libraries.Library
import java.io.File
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ContentEntry

class HighlightingWithDependentLibrariesTest : KotlinLightCodeInsightFixtureTestCase() {
    private val TEST_DATA_PATH = PluginTestCaseBase.TEST_DATA_DIR + "/highlightingWithDependentLibraries"

    override fun getProjectDescriptor() = object : KotlinLightProjectDescriptor() {
        override fun configureModule(module: Module, model: ModifiableRootModel) {
            val compiledJar1 = MockLibraryUtil.compileLibraryToJar("$TEST_DATA_PATH/lib1", "lib1", false, false)
            val compiledJar2 = MockLibraryUtil.compileLibraryToJar("$TEST_DATA_PATH/lib2", "lib2", false, false, compiledJar1.canonicalPath)

            model.addLibraryEntry(createLibrary(compiledJar1, "baseLibrary"))
            model.addLibraryEntry(createLibrary(compiledJar2, "dependentLibrary"))
        }

        private fun createLibrary(jarFile: File, name: String): Library {
            val library = LibraryTablesRegistrar.getInstance()!!.getLibraryTable(project).createLibrary(name)!!
            val model = library.modifiableModel
            model.addRoot(VfsUtil.getUrlForLibraryRoot(jarFile), OrderRootType.CLASSES)
            model.commit()
            return library
        }
    }

    fun testHighlightingWithDependentLibraries() {
        myFixture.configureByFile("$TEST_DATA_PATH/module/usingLibs.kt")
        myFixture.checkHighlighting(false, false, false)
    }
}
