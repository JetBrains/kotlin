/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.psi.KtElement

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsResolveExtensionInfoProvider {
    public fun resolveExtensionScopeWithTopLevelDeclarations(): KaScope

    public fun isResolveExtensionFile(virtualFile: VirtualFile): Boolean

    public fun isFromResolveExtension(element: KtElement): Boolean

    public fun resolveExtensionNavigationElements(element: KtElement): Collection<PsiElement>
}
