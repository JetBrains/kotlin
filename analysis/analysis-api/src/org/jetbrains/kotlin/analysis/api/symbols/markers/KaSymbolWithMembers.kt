/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

public interface KaSymbolWithMembers : KaSymbolWithDeclarations

@Deprecated("Use 'KaSymbolWithMembers' instead", ReplaceWith("KaSymbolWithMembers"))
public typealias KtSymbolWithMembers = KaSymbolWithMembers

public interface KaSymbolWithDeclarations : KaSymbol

@Deprecated("Use 'KaSymbolWithDeclarations' instead", ReplaceWith("KaSymbolWithDeclarations"))
public typealias KtSymbolWithDeclarations = KaSymbolWithDeclarations