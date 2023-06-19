/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

private val allowedFakeElementKinds = setOf(
    KtFakeSourceElementKind.FromUseSiteTarget,
    KtFakeSourceElementKind.PropertyFromParameter,
    KtFakeSourceElementKind.ItLambdaParameter,
    KtFakeSourceElementKind.EnumGeneratedDeclaration,
    KtFakeSourceElementKind.DataClassGeneratedMembers,
    KtFakeSourceElementKind.ImplicitConstructor,
)

internal fun FirElement.getAllowedPsi() = when (val source = source) {
    null -> null
    is KtRealPsiSourceElement -> source.psi
    is KtFakeSourceElement -> if (source.kind in allowedFakeElementKinds) psi else null
    else -> null
}

fun FirElement.findPsi(): PsiElement? =
    getAllowedPsi()

fun FirBasedSymbol<*>.findPsi(): PsiElement? =
    fir.findPsi() ?: FirSyntheticFunctionInterfaceSourceProvider.findPsi(fir)

/**
 * Finds [PsiElement] which will be used as go-to referenced element for [KtPsiReference]
 * For data classes & enums generated members like `copy` `componentN`, `values` it will return corresponding enum/data class
 * Otherwise, behaves the same way as [findPsi] returns exact PSI declaration corresponding to passed [FirDeclaration]
 */
fun FirDeclaration.findReferencePsi(): PsiElement? {
    return psi ?: FirSyntheticFunctionInterfaceSourceProvider.findPsi(this)
}