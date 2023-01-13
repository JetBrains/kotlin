/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithTypeParameters

/**
 * Represents a symbol of declaration which can be directly expressed in source code.
 * Eg, classes, type parameters or functions are [KtDeclarationSymbol], but files and packages are not
 */
public sealed interface KtDeclarationSymbol : KtSymbol, KtSymbolWithTypeParameters, KtAnnotatedSymbol