/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleFinder
import java.util.concurrent.ConcurrentHashMap

/**
 * Expands the target of a module import declaration (`import module M;`, JLS 7.5.5) into the
 * packages it brings into on-demand scope.
 */
internal fun interface JavaModuleImportedPackages {
    fun forModule(moduleName: String): List<FqName>

    companion object {
        val EMPTY: JavaModuleImportedPackages = JavaModuleImportedPackages { emptyList() }
    }
}

/**
 * The unqualified exports of the named module plus, transitively, those of every module it `requires transitive` (JLS 7.5.5).
 */
internal class JavaModuleImportedPackagesOverModuleGraph(
    private val moduleFinder: JavaModuleFinder,
) : JavaModuleImportedPackages {
    private val cache = ConcurrentHashMap<String, List<FqName>>()

    override fun forModule(moduleName: String): List<FqName> = cache.getOrPut(moduleName) { compute(moduleName) }

    private fun compute(moduleName: String): List<FqName> {
        val packages = LinkedHashSet<FqName>()
        val visited = mutableSetOf<String>()

        fun visit(name: String) {
            if (!visited.add(name)) return
            val moduleInfo = (moduleFinder.findModule(name) as? JavaModule.Explicit)?.moduleInfo ?: return
            for ((packageFqName, toModules) in moduleInfo.exports) {
                if (toModules.isEmpty()) packages.add(packageFqName)
            }
            for ((moduleName, isTransitive) in moduleInfo.requires) {
                if (isTransitive) visit(moduleName)
            }
        }

        visit(moduleName)
        return packages.toList()
    }
}
