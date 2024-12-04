/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.fir.declarations.FirDeclaration

/**
 * This interface provides an ability to calculate declarations lazily to avoid
 * problems with right-away initialization (like contract violations and redundant calculations in the Analysis API mode).
 *
 * See [KT-70474](https://youtrack.jetbrains.com/issue/KT-70474) for details.
 *
 * TODO: the lazy declarations is a workaround for KT-55387, some non-lazy solution should probably be used instead
 *
 * @see FirEmptyJavaDeclarationList
 */
interface FirJavaDeclarationList {
    val declarations: List<FirDeclaration>
}

object FirEmptyJavaDeclarationList : FirJavaDeclarationList {
    override val declarations: List<FirDeclaration> get() = emptyList()
}
