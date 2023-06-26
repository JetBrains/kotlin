/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.psi.KtTypeReference

internal fun buildFirUserTypeRef(
    typeReference: KtTypeReference,
    session: FirSession,
    baseScopeProvider: FirScopeProvider
): FirUserTypeRef {
    val builder = object : PsiRawFirBuilder(session, baseScopeProvider) {
        fun build(): FirUserTypeRef = Visitor().visitTypeReference(typeReference, null) as FirUserTypeRef
    }
    builder.context.packageFqName = typeReference.containingKtFile.packageFqName
    return builder.build()
}
