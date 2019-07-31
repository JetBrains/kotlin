/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement

// TODO: remove : FirElement
enum class FirResolvePhase : FirElement {
    RAW_FIR,
    IMPORTS,
    SUPER_TYPES,
    TYPES,
    STATUS,
    IMPLICIT_TYPES_BODY_RESOLVE,
    BODY_RESOLVE;

    val prev: FirResolvePhase get() = values()[ordinal - 1]

    val next: FirResolvePhase get() = values()[ordinal + 1]

    override val psi: PsiElement?
        get() = null

    companion object {
        // Short-cut
        val DECLARATIONS = STATUS
    }
}