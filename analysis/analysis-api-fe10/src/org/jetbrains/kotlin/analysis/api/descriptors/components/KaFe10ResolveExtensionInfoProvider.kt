/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KaResolveExtensionInfoProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KaEmptyScope
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.psi.KtElement

// Resolve extensions are not supported on FE1.0, so return empty results.
internal class KaFe10ResolveExtensionInfoProvider(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaSessionComponent<KaFe10Session>(), KaResolveExtensionInfoProvider, KaFe10SessionComponent {
    override val resolveExtensionScopeWithTopLevelDeclarations: KaScope
        get() = withValidityAssertion { KaEmptyScope(token) }

    override val VirtualFile.isResolveExtensionFile: Boolean
        get() = withValidityAssertion { false }

    override val KtElement.isFromResolveExtension: Boolean
        get() = withValidityAssertion { containingKtFile.virtualFile?.isResolveExtensionFile == true }

    override val KtElement.resolveExtensionNavigationElements: Collection<PsiElement>
        get() = withValidityAssertion { emptyList() }
}