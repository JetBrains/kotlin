/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

/**
 * A [KaSymbol] which may contain declarations. These declarations may be accessed through the [KaScope][org.jetbrains.kotlin.analysis.api.scopes.KaScope]s
 * provided by [KaScopeProvider][org.jetbrains.kotlin.analysis.api.components.KaScopeProvider].
 */
public interface KaDeclarationContainerSymbol : KaSymbol {
    override fun createPointer(): KaSymbolPointer<KaDeclarationContainerSymbol>
}
