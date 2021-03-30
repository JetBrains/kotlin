/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.applicator

import com.intellij.psi.PsiElement

/**
 * Data which [HLApplicator] is needed to perform the fix
 *
 * Created by [HLApplicatorInputProvider] or via [org.jetbrains.kotlin.idea.fir.api.fixes.HLDiagnosticFixFactory]
 *
 * Should not store inside
 * - Everything that came from [org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession] like :
 *      - [org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol] consider using [org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer] instead
 *      - [org.jetbrains.kotlin.idea.frontend.api.types.KtType]
 *      - [org.jetbrains.kotlin.idea.frontend.api.calls.KtCall]
 * - [org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession] instance itself
 * - [PsiElement] consider using [com.intellij.psi.SmartPsiElementPointer] instead
 *
 */
interface HLApplicatorInput {
    fun isValidFor(psi: PsiElement): Boolean = true
}
