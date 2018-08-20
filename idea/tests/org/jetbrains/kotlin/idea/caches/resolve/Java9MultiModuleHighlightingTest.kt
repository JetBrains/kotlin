/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
