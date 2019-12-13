/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.test.TestJdkKind

/**
 * Dependencies: 'main' -> 'm2', 'main' -> 'm4', 'main' -> 'm5', 'main' -> 'm6' => 'm7'
 */
object KotlinMultiModuleJava9ProjectDescriptor : DefaultLightProjectDescriptor() {
    enum class ModuleDescriptor(internal val moduleName: String, internal val rootName: String) {
        // Dependent for: none, Depends on: M2, M4, M5, M6
        MAIN(TEST_MODULE_NAME, "/not_used/"),

        // Dependent for: MAIN, Depends on: none
        M2("${TEST_MODULE_NAME}_m2", "src_m2"),

        // Dependent for: none, Depends on: none
        M3("${TEST_MODULE_NAME}_m3", "src_m3"),

        // Dependent for: MAIN, Depends on: none
        M4("${TEST_MODULE_NAME}_m4", "src_m4"),

        // Dependent for: MAIN, Depends on: none
        M5("${TEST_MODULE_NAME}_m5", "src_m5"),

        // Dependent for: MAIN, Depends on: M7
        M6("${TEST_MODULE_NAME}_m6", "src_m6"),

        // Dependent for: M6, Depends on: none
        M7("${TEST_MODULE_NAME}_m7", "src_m7");

        fun root(): VirtualFile =
            if (this == MAIN) LightPlatformTestCase.getSourceRoot() else TempFileSystem.getInstance().findFileByPath("/$rootName")!!

        fun testRoot(): VirtualFile? = if (this == MAIN) TempFileSystem.getInstance().findFileByPath("/test_src")!! else null
    }

    override fun getSdk(): Sdk = PluginTestCaseBase.jdk(TestJdkKind.FULL_JDK_9)

    override fun setUpProject(project: Project, handler: SetupHandler) {
        super.setUpProject(project, handler)

        runWriteAction {
            val main = ModuleManager.getInstance(project).findModuleByName(TEST_MODULE_NAME)!!

            val m2 = makeModule(project, ModuleDescriptor.M2)
            ModuleRootModificationUtil.addDependency(main, m2)

            makeModule(project, ModuleDescriptor.M3)

            val m4 = makeModule(project, ModuleDescriptor.M4)
            ModuleRootModificationUtil.addDependency(main, m4)

            val m5 = makeModule(project, ModuleDescriptor.M5)
            ModuleRootModificationUtil.addDependency(main, m5)

            val m6 = makeModule(project, ModuleDescriptor.M6)
            ModuleRootModificationUtil.addDependency(main, m6)

            val m7 = makeModule(project, ModuleDescriptor.M7)
            ModuleRootModificationUtil.addDependency(m6, m7, DependencyScope.COMPILE, true)
        }
    }

    private fun makeModule(project: Project, descriptor: ModuleDescriptor): Module {
        val path = FileUtil.join(FileUtil.getTempDirectory(), "${descriptor.moduleName}.iml")
        val module = createModule(project, path)
        val sourceRoot = createSourceRoot(module, descriptor.rootName)
        createContentEntry(module, sourceRoot)
        return module
    }

    override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
        model.getModuleExtension(LanguageLevelModuleExtension::class.java).languageLevel = LanguageLevel.JDK_1_9
        if (module.name == TEST_MODULE_NAME) {
            val testRoot = createSourceRoot(module, "test_src")
            registerSourceRoot(module.project, testRoot)
            model.addContentEntry(testRoot).addSourceFolder(testRoot, JavaSourceRootType.TEST_SOURCE)
        }
    }

    fun cleanupSourceRoots() = runWriteAction {
        ModuleDescriptor.values().asSequence()
            .filter { it != ModuleDescriptor.MAIN }
            .flatMap { it.root().children.asSequence() }
            .plus(ModuleDescriptor.MAIN.testRoot()!!.children.asSequence())
            .forEach { it.delete(this) }
    }
}