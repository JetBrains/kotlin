/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols.markers

interface KtSymbolWithVisibility {
    val visibility: KtSymbolVisibility
}

sealed class KtSymbolVisibility {
    object PUBLIC : KtSymbolVisibility()
    object PRIVATE : KtSymbolVisibility()
    object PRIVATE_TO_THIS : KtSymbolVisibility()
    object PROTECTED : KtSymbolVisibility()
    object INTERNAL : KtSymbolVisibility()
    object UNKNOWN : KtSymbolVisibility()
    object LOCAL : KtSymbolVisibility()
}

fun KtSymbolVisibility.isPrivateOrPrivateToThis() = this == KtSymbolVisibility.PRIVATE || this == KtSymbolVisibility.PRIVATE_TO_THIS