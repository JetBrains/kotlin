/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.caches.resolve

import com.intellij.testFramework.ModuleTestCase
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.DependencyScope
import junit.framework.Assert
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.command.WriteCommandAction

class IdeaModuleInfoTest : ModuleTestCase() {

    fun testSimpleModuleDependency() {
        val (a, b) = modules()
        b.addDependency(a)

        b.source.assertDependenciesEqual(b.source, a.source)
        assertDoesntContain(a.source.dependencies(), b.source)
    }

    fun testCircularDependency() {
        val (a, b) = modules()

        b.addDependency(a)
        a.addDependency(b)

        a.source.assertDependenciesEqual(a.source, b.source)
        b.source.assertDependenciesEqual(b.source, a.source)
    }

    fun testExportedDependency() {
        val (a, b, c) = modules()

        b.addDependency(a, exported = true)
        c.addDependency(b)

        a.source.assertDependenciesEqual(a.source)
        b.source.assertDependenciesEqual(b.source, a.source)
        c.source.assertDependenciesEqual(c.source, b.source, a.source)
    }

    fun testRedundantExportedDependency() {
        val (a, b, c) = modules()

        b.addDependency(a, exported = true)
        c.addDependency(a)
        c.addDependency(b)

        a.source.assertDependenciesEqual(a.source)
        b.source.assertDependenciesEqual(b.source, a.source)
        c.source.assertDependenciesEqual(c.source, a.source, b.source)
    }

    fun testCircularExportedDependency() {
        val (a, b, c) = modules()

        b.addDependency(a, exported = true)
        c.addDependency(b, exported = true)
        a.addDependency(c, exported = true)

        a.source.assertDependenciesEqual(a.source, c.source, b.source)
        b.source.assertDependenciesEqual(b.source, a.source, c.source)
        c.source.assertDependenciesEqual(c.source, b.source, a.source)
    }

    fun testSimpleLibDependency() {
        val a = module("a")
        val lib = projectLibrary()
        a.addDependency(lib)

        a.source.assertDependenciesEqual(a.source, lib.classes)
    }

    fun testCircularExportedDependencyWithLib() {
        val (a, b, c) = modules()

        val lib = projectLibrary()

        a.addDependency(lib)

        b.addDependency(a, exported = true)
        c.addDependency(b, exported = true)
        a.addDependency(c, exported = true)

        b.addDependency(lib)
        c.addDependency(lib)

        a.source.assertDependenciesEqual(a.source, lib.classes, c.source, b.source)
        b.source.assertDependenciesEqual(b.source, a.source, c.source, lib.classes)
        c.source.assertDependenciesEqual(c.source, b.source, a.source, lib.classes)
    }

    fun testSeveralModulesExportLibs() {
        val (a, b, c) = modules()

        val lib1 = projectLibrary("lib1")
        val lib2 = projectLibrary("lib2")

        a.addDependency(lib1, exported = true)
        b.addDependency(lib2, exported = true)
        c.addDependency(a)
        c.addDependency(b)

        c.source.assertDependenciesEqual(c.source, a.source, lib1.classes, b.source, lib2.classes)
    }

    fun testSeveralModulesExportSameLib() {
        val (a, b, c) = modules()

        val lib = projectLibrary()

        a.addDependency(lib, exported = true)
        b.addDependency(lib, exported = true)
        c.addDependency(a)
        c.addDependency(b)

        c.source.assertDependenciesEqual(c.source, a.source, lib.classes, b.source)
    }

    fun testRuntimeDependency() {
        val (a, b) = modules()

        b.addDependency(a, dependencyScope = DependencyScope.RUNTIME)
        b.addDependency(projectLibrary(), dependencyScope = DependencyScope.RUNTIME)

        b.source.assertDependenciesEqual(b.source)
    }

    fun testProvidedDependency() {
        val (a, b) = modules()
        val lib = projectLibrary()

        b.addDependency(a, dependencyScope = DependencyScope.PROVIDED)
        b.addDependency(lib, dependencyScope = DependencyScope.PROVIDED)

        b.source.assertDependenciesEqual(b.source, a.source, lib.classes)
    }

    private fun Module.addDependency(
            other: Module,
            dependencyScope: DependencyScope = DependencyScope.COMPILE,
            exported: Boolean = false
    ) = ModuleRootModificationUtil.addDependency(this, other, dependencyScope, exported)

    private val Module.source: ModuleProductionSourceInfo
            get() = productionSourceInfo()
    private val Library.classes: LibraryInfo
            get() = LibraryInfo(getProject()!!, this)

    private fun Module.addDependency(
            lib: Library,
            dependencyScope: DependencyScope = DependencyScope.COMPILE,
            exported: Boolean = false
    ) = ModuleRootModificationUtil.addDependency(this, lib, dependencyScope, exported)

    private fun module(name: String): Module {
        return createModuleFromTestData(createTempDirectory()!!.getAbsolutePath(), name, StdModuleTypes.JAVA, false)!!
    }

    private fun modules(name1: String = "a", name2: String = "b", name3: String = "c") = Triple(module(name1), module(name2), module(name3))

    private fun IdeaModuleInfo.assertDependenciesEqual(vararg dependencies: IdeaModuleInfo) {
        Assert.assertEquals(dependencies.toList(), this.dependencies())
    }

    private fun projectLibrary(name: String = "lib"): Library {
        val libraryTable = ProjectLibraryTable.getInstance(myProject)!!
        return WriteCommandAction.runWriteCommandAction<Library>(myProject) {
            libraryTable.createLibrary(name)
        }!!
    }
}
