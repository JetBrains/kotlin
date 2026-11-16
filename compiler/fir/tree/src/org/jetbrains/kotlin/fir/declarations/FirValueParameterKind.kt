/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

enum class FirValueParameterKind {
    Regular,
    ContextParameter,

    // In K2, we don't support context receivers in resolution anymore, but we keep them in the FIR
    // for a context parameter without a name in the sources (the former syntax of context receivers)
    // and for a context receiver in the metadata that has no context parameters: the metadata of a class,
    // or of a callable compiled by K1 or before Kotlin 2.2.0. K2 writes context parameters to the metadata
    // of callables since 2.2.0 (KT-74546).
    LegacyContextReceiver,
}

fun FirValueParameter.isLegacyContextReceiver(): Boolean {
    return valueParameterKind == FirValueParameterKind.LegacyContextReceiver
}