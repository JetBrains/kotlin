/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.PsiHandlingMode
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.psi.KtTypeReference

internal fun buildFirUserTypeRef(
    typeReference: KtTypeReference,
    session: FirSession,
    baseScopeProvider: FirScopeProvider
): FirUserTypeRef {
    val builder = object : RawFirBuilder(session, baseScopeProvider, psiMode = PsiHandlingMode.IDE) {
        fun build(): FirUserTypeRef = Visitor().visitTypeReference(typeReference, Unit) as FirUserTypeRef
    }
    builder.context.packageFqName = typeReference.containingKtFile.packageFqName
    return builder.build()
}
