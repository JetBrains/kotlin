/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

public class KaBaseContentScopeProvider : KaContentScopeProvider {
    override fun getRefinedContentScope(module: KaModule): GlobalSearchScope {
        var resultingScope = module.baseContentScope

        val refiners = KotlinContentScopeRefiner.getRefiners(module.project)

        if (refiners.isEmpty()) return resultingScope

        refiners.flatMap {
            it.getEnlargementScopes(module).filter { scope ->
                !GlobalSearchScope.isEmptyScope(scope)
            }
        }.forEach {
            resultingScope = resultingScope.union(it)
        }

        refiners.flatMap {
            it.getRestrictionScopes(module).filter { scope ->
                !GlobalSearchScope.isEmptyScope(scope)
            }
        }.forEach {
            resultingScope = resultingScope.intersectWith(GlobalSearchScope.notScope(it))
        }

        return resultingScope
    }
}