/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KtResolveExtensionInfoProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KtEmptyScope
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.psi.KtElement

// Resolve extensions are not supported on FE1.0, so return empty results.
internal class KtFe10ResolveExtensionInfoProvider(
    override val analysisSession: KtFe10AnalysisSession,
) : KtResolveExtensionInfoProvider(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun getResolveExtensionScopeWithTopLevelDeclarations(): KtScope {
        return KtEmptyScope(token)
    }

    override fun isResolveExtensionFile(file: VirtualFile): Boolean = false

    override fun getResolveExtensionNavigationElements(originalPsi: KtElement): Collection<PsiElement> = emptyList()
}