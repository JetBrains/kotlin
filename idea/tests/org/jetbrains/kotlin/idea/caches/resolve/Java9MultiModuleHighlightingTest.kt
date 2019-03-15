/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.TestJdkKind.FULL_JDK_9

class Java9MultiModuleHighlightingTest : AbstractMultiModuleHighlightingTest() {
    override fun getTestDataPath(): String = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleHighlighting/java9/"

    private fun module(name: String): Module = super.module(name, FULL_JDK_9, false)

    fun testSimpleModuleExportsPackage() {
        module("main").addDependency(module("dependency"))
        checkHighlightingInProject()
    }

    fun testSimpleLibraryExportsPackage() {
        // -Xallow-kotlin-package to avoid "require kotlin.stdlib" in module-info.java
        val library = MockLibraryUtil.compileJvmLibraryToJar(
            testDataPath + "${getTestName(true)}/library", "library",
            extraOptions = listOf("-jdk-home", KotlinTestUtils.getJdk9Home().path, "-Xallow-kotlin-package"),
            useJava9 = true
        )

        module("main").addLibrary(library, "library")
        checkHighlightingInProject()
    }

    fun testNamedDependsOnUnnamed() {
        module("main").addDependency(module("dependency"))
        checkHighlightingInProject()
    }

    fun testUnnamedDependsOnNamed() {
        module("main").addDependency(module("dependency"))
        checkHighlightingInProject()
    }

    fun testDeclarationKinds() {
        module("main").addDependency(module("dependency"))
        checkHighlightingInProject()
    }

    fun testExportsTo() {
        val d = module("dependency")
        module("first").addDependency(d)
        module("second").addDependency(d)
        module("unnamed").addDependency(d)
        checkHighlightingInProject()
    }

    fun testExportedPackageIsInaccessibleWithoutRequires() {
        module("main").addDependency(module("dependency"))
        checkHighlightingInProject()
    }

    fun testTypealiasToUnexported() {
        module("main").addDependency(module("dependency"))
        checkHighlightingInProject()
    }

    fun testCyclicDependency() {
        val a = module("moduleA")
        val b = module("moduleB")
        val c = module("moduleC")
        module("main").addDependency(a).addDependency(b).addDependency(c)
        checkHighlightingInProject()
    }
}
