/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinContentScopeRefiner
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider

internal class KaResolveExtensionToContentScopeRefinerBridge : KotlinContentScopeRefiner {
    override fun getRestrictionScopes(module: KaModule): List<GlobalSearchScope> =
        KaResolveExtensionProvider.provideExtensionsFor(module).map { resolveExtension ->
            GlobalSearchScope.notScope(resolveExtension.getShadowedScope())
        }
}
