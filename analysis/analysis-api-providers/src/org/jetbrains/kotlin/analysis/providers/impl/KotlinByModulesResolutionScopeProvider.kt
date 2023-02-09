/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.allDirectDependencies
import org.jetbrains.kotlin.analysis.providers.KotlinResolutionScopeProvider

public class KotlinByModulesResolutionScopeProvider : KotlinResolutionScopeProvider() {
    override fun getResolutionScope(module: KtModule): GlobalSearchScope {
        val allModules = buildList {
            add(module)
            addAll(module.allDirectDependencies())
        }
        return GlobalSearchScope.union(allModules.map { it.contentScope })
    }
}