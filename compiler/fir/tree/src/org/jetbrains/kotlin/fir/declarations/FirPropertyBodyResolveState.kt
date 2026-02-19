/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol

// Semantically all states here are parts of FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE and just BODY_RESOLVE
enum class FirPropertyBodyResolveState {
    NOTHING_RESOLVED,
    INITIALIZER_RESOLVED,
    INITIALIZER_AND_GETTER_RESOLVED,
    ALL_BODIES_RESOLVED,
}

val FirProperty.isEffectivelyLocal: Boolean get() = symbol is FirLocalPropertySymbol || isReplSnippetDeclaration == true
