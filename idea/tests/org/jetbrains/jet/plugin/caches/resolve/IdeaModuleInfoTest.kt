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
import com.intellij.testFramework.UsefulTestCase
import junit.framework.Assert
import com.intellij.openapi.roots.libraries.LibraryTable
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


    //NOTE: wrapper classes to reduce boilerplate in test cases
    private class ModuleDef(val ideaModule: Module) {
        val source = ideaModule.toSourceInfo()
    }

    private inner class LibraryDef(val ideaLibrary: Library) {
        val classes = LibraryInfo(getProject()!!, ideaLibrary)
    }

    private fun ModuleDef.addDependency(
            other: ModuleDef,
            dependencyScope: DependencyScope = DependencyScope.COMPILE,
            exported: Boolean = false
    ) = ModuleRootModificationUtil.addDependency(this.ideaModule, other.ideaModule, dependencyScope, exported)

    private fun ModuleDef.addDependency(
            lib: LibraryDef,
            dependencyScope: DependencyScope = DependencyScope.COMPILE,
            exported: Boolean = false
    ) = ModuleRootModificationUtil.addDependency(this.ideaModule, lib.ideaLibrary, dependencyScope, exported)

    private fun module(name: String): ModuleDef {
        val ideaModule = createModuleFromTestData(createTempDirectory()!!.getAbsolutePath(), name, StdModuleTypes.JAVA, false)!!
        return ModuleDef(ideaModule)
    }

    private fun modules(name1: String = "a", name2: String = "b", name3: String = "c") = Triple(module(name1), module(name2), module(name3))

    private fun IdeaModuleInfo.assertDependenciesEqual(vararg dependencies: IdeaModuleInfo) {
        Assert.assertEquals(dependencies.toList(), this.dependencies())
    }

    private fun projectLibrary(name: String = "lib"): LibraryDef {
        val libraryTable = ProjectLibraryTable.getInstance(myProject)!!
        val library = WriteCommandAction.runWriteCommandAction<Library>(myProject) {
            libraryTable.createLibrary(name)
        }!!
        return LibraryDef(library)
    }
}
