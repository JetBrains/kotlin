/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

enum class FirValueParameterKind {
    Regular,
    ContextParameter,

    // In K2, we don't support context receivers in resolution anymore,
    // but we keep them in the FIR as long as we support parsing them
    // (which is at least as long as K1 is supported).
    LegacyContextReceiver,
}

fun FirValueParameter.isLegacyContextReceiver(): Boolean {
    return valueParameterKind == FirValueParameterKind.LegacyContextReceiver
}