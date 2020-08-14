/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtCommonSymbolModality
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolModality
import org.jetbrains.kotlin.psi.KtDeclaration

internal inline fun <reified M : KtSymbolModality> Modality?.getSymbolModality(): M = when (this) {
    Modality.FINAL -> KtCommonSymbolModality.FINAL
    Modality.OPEN -> KtCommonSymbolModality.OPEN
    Modality.ABSTRACT -> KtCommonSymbolModality.ABSTRACT
    Modality.SEALED -> KtSymbolModality.SEALED
    null -> error("Symbol modality should not be null, looks like the fir symbol was not properly resolved")
} as? M ?: error("Sealed modality can only be applied to class")