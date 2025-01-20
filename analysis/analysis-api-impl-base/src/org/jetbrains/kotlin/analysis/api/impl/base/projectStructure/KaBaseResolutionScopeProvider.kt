/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinGlobalSearchScopeMerger
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider

class KaBaseResolutionScopeProvider : KaResolutionScopeProvider {
    override fun getResolutionScope(module: KaModule): GlobalSearchScope {
        val moduleWithDependentScopes = getResolutionScopes(module)
        return KotlinGlobalSearchScopeMerger.getInstance(module.project).union(moduleWithDependentScopes)
    }

    @OptIn(KaPlatformInterface::class)
    private fun getResolutionScopes(module: KaModule): List<GlobalSearchScope> {
        val modules = buildSet {
            add(module)
            addAll(module.allDirectDependencies())
            if (module is KaLibrarySourceModule) {
                add(module.binaryLibrary)
            }
        }

        return buildList {
            modules.mapTo(this) { it.contentScope }
            if (modules.none { it is KaBuiltinsModule }) {
                // Workaround for KT-72988
                // after fixed, it should probably only be added if `module.targetPlatform.hasCommonKotlinStdlib()`
                add(createBuiltinsScope(module.project))
            }
        }
    }

    private fun createBuiltinsScope(project: Project): GlobalSearchScope {
        return BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)
    }
}
