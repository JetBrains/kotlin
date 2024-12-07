/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.utils.topologicalSort

public class KotlinByModulesResolutionScopeProvider : KotlinResolutionScopeProvider {
    override fun getResolutionScope(module: KaModule): GlobalSearchScope {
        val allModules = topologicalSort(listOf(module)) {
            buildList {
                addAll(directDependsOnDependencies)
                addAll(directFriendDependencies)
                addAll(directRegularDependencies)
            }
        }

        val scope = GlobalSearchScope.union(allModules.map { it.contentScope })

        // workaround for KT-72988
        val builtinsScope = BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(module.project)
        return scope.uniteWith(builtinsScope)
    }
}
