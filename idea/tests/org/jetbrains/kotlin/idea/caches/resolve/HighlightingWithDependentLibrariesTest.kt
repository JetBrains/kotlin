/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.MockLibraryUtil
import java.io.File

class HighlightingWithDependentLibrariesTest : KotlinLightCodeInsightFixtureTestCase() {
    private val TEST_DATA_PATH = PluginTestCaseBase.TEST_DATA_DIR + "/highlightingWithDependentLibraries"

    override fun getProjectDescriptor() = object : KotlinLightProjectDescriptor() {
        override fun configureModule(module: Module, model: ModifiableRootModel) {
            val compiledJar1 =
                MockLibraryUtil.compileJvmLibraryToJar("$TEST_DATA_PATH/lib1", "lib1")
            val compiledJar2 =
                MockLibraryUtil.compileJvmLibraryToJar("$TEST_DATA_PATH/lib2", "lib2", extraClasspath = listOf(compiledJar1.canonicalPath))

            model.addLibraryEntry(createLibrary(module.project, compiledJar1, "baseLibrary"))
            model.addLibraryEntry(createLibrary(module.project, compiledJar2, "dependentLibrary"))
        }

        private fun createLibrary(project: Project, jarFile: File, name: String): Library {
            val library = LibraryTablesRegistrar.getInstance()!!.getLibraryTable(project).createLibrary(name)
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
