/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

enum class FirValueParameterKind {
    Regular,
    ContextParameter,

    // TODO(KT-72994) remove with the rest of context receivers.
    //  Afterwards, the enum can be replaced with a bool flag `isContextParameter`.
    LegacyContextReceiver,
}

fun FirValueParameter.isLegacyContextReceiver(): Boolean {
    return valueParameterKind == FirValueParameterKind.LegacyContextReceiver
}