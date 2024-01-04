/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.KotlinResolutionScopeProvider
import org.jetbrains.kotlin.utils.topologicalSort

public class KotlinByModulesResolutionScopeProvider : KotlinResolutionScopeProvider() {
    override fun getResolutionScope(module: KtModule): GlobalSearchScope {
        val allModules = topologicalSort(listOf(module)) {
            buildList {
                addAll(directDependsOnDependencies)
                addAll(directFriendDependencies)
                addAll(directRegularDependencies)
            }
        }

        return GlobalSearchScope.union(allModules.map { it.contentScope })
    }
}
