/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaResolveExtensionInfoProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsResolveExtensionInfoProvider
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.analysis.api.resolve.extensions.isFromResolveExtension as isFromResolveExtensionEndpoint
import org.jetbrains.kotlin.analysis.api.resolve.extensions.isResolveExtensionFile as isResolveExtensionFileEndpoint
import org.jetbrains.kotlin.analysis.api.resolve.extensions.resolveExtensionNavigationElements as resolveExtensionNavigationElementsEndpoint
import org.jetbrains.kotlin.analysis.api.resolve.extensions.resolveExtensionScopeWithTopLevelDeclarations as resolveExtensionScopeWithTopLevelDeclarationsEndpoint

/**
 * Routes the legacy [KaResolveExtensionInfoProvider] surface through the new public `context(session: KaSession)` endpoints in the
 * `org.jetbrains.kotlin.analysis.api.resolve.extensions` package, which in turn reach the [KaInternalsResolveExtensionInfoProvider] proxy.
 */
@OptIn(KaExperimentalApi::class)
internal class KaResolveExtensionInfoProviderBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaResolveExtensionInfoProvider {
    override val resolveExtensionScopeWithTopLevelDeclarations: KaScope
        get() = context(analysisSession) { resolveExtensionScopeWithTopLevelDeclarationsEndpoint }

    override val VirtualFile.isResolveExtensionFile: Boolean
        get() = context(analysisSession) { isResolveExtensionFileEndpoint }

    override val KtElement.isFromResolveExtension: Boolean
        get() = context(analysisSession) { isFromResolveExtensionEndpoint }

    override val KtElement.resolveExtensionNavigationElements: Collection<PsiElement>
        get() = context(analysisSession) { resolveExtensionNavigationElementsEndpoint }
}
