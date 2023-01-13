/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer

@NoMutableState
data class IdeSessionComponents(val signatureComposer: FirBasedSignatureComposer) : FirSessionComponent {
    companion object {
        fun create(): IdeSessionComponents = IdeSessionComponents(
            signatureComposer = FirBasedSignatureComposer(FirJvmKotlinMangler())
        )
    }
}

val FirSession.ideSessionComponents: IdeSessionComponents by FirSession.sessionComponentAccessor()
