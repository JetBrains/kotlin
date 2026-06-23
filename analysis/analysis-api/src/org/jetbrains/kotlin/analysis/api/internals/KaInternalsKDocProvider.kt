/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.kdoc.psi.api.KDocCommentDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNonPublicApi

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
@OptIn(KtNonPublicApi::class)
public interface KaInternalsKDocProvider {
    public fun findKDoc(declaration: KtDeclaration): KDocCommentDescriptor?

    public fun findKDoc(symbol: KaDeclarationSymbol): KDocCommentDescriptor?
}
