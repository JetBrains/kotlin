/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import java.io.File

class KotlinJdkAndMultiplatformStdlibDescriptor private constructor(private val withSources: Boolean) : KotlinLightProjectDescriptor() {
    override fun getModuleType(): ModuleType<*> = StdModuleTypes.JAVA

    override fun getSdk(): Sdk? = PluginTestCaseBase.mockJdk8()

    override fun configureModule(module: Module, model: ModifiableRootModel) {
        model.addLib(
            STDLIB_COMMON_LIB_NAME,
            withSources,
            ForTestCompileRuntime.stdlibCommonForTests(),
            ForTestCompileRuntime.stdlibCommonSourcesForTests()
        )

        model.addLib(
            STDLIB_LIB_NAME,
            withSources,
            ForTestCompileRuntime.runtimeJarForTests(),
            ForTestCompileRuntime.stdlibMavenSourcesJarForTests()
        )
    }

    companion object {
        val JDK_AND_MULTIPLATFORM_STDLIB_WITH_SOURCES = KotlinJdkAndMultiplatformStdlibDescriptor(true)

        private fun ModifiableRootModel.addLib(name: String, withSources: Boolean, classesJar: File, sourcesJar: File?): Library {
            val editor = NewLibraryEditor().apply {
                this.name = name
                addRoot(VfsUtil.getUrlForLibraryRoot(classesJar), OrderRootType.CLASSES)

                if (withSources && sourcesJar != null) {
                    addRoot(VfsUtil.getUrlForLibraryRoot(sourcesJar), OrderRootType.SOURCES)
                }
            }

            return ConfigLibraryUtil.addLibrary(editor, this)
        }

        private const val STDLIB_COMMON_LIB_NAME = "stdlib-common"
        private const val STDLIB_LIB_NAME = "stdlib"
    }
}