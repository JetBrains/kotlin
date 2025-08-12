/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinContentScopeRefiner
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtension
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider

@KaImplementationDetail
class KaResolveExtensionToContentScopeRefinerBridge : KotlinContentScopeRefiner {
    override fun getEnlargementScopes(module: KaModule): List<GlobalSearchScope> =
        withResolveExtensionsFor(module) { extensions ->
            if (extensions.isNotEmpty()) {
                listOf(KaBaseResolveExtensionGeneratedFilesScope(listOf(module)))
            } else {
                listOf()
            }
        }

    override fun getRestrictionScopes(module: KaModule): List<GlobalSearchScope> =
        withResolveExtensionsFor(module) { extensions ->
            extensions.map { GlobalSearchScope.notScope(it.getShadowedScope()) }
        }

    companion object {
        private inline fun <T> withResolveExtensionsFor(
            module: KaModule,
            block: (List<KaResolveExtension>) -> T,
        ): T {
            var disposable: Disposable? = null
            try {
                val extensions =
                    KaResolveExtensionProvider.provideExtensionsFor(module) {
                        Disposer.newDisposable("KaResolveExtensionToContentScopeRefinerBridge")
                            .also { disposable = it }
                    }
                return block(extensions)
            } finally {
                if (disposable != null) {
                    Disposer.dispose(disposable)
                }
            }
        }
    }
}
