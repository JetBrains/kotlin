/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols.markers

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.name.Name

interface KtNamedSymbol : KtSymbol {
    val name: Name
}

interface KtTypedSymbol : KtSymbol {
    val type: KtType
}

interface KtSymbolWithTypeParameters {
    val typeParameters: List<KtTypeParameterSymbol>
}