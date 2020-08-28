/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer

data class IdeSessionComponents(val signatureComposer: FirBasedSignatureComposer): FirSessionComponent {
    companion object {
        fun create(session: FirSession) = IdeSessionComponents(
            signatureComposer = FirBasedSignatureComposer(FirJvmKotlinMangler(session))
        )
    }
}
val FirSession.ideSessionComponents: IdeSessionComponents by FirSession.sessionComponentAccessor()
