/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.utils.Jsr305State

class FirJsr305StateContainer(val jsr305State: Jsr305State) : FirSessionComponent {
    companion object {
        val Default: FirJsr305StateContainer = FirJsr305StateContainer(Jsr305State.DEFAULT)
    }
}

val FirSession.jsr305StateContainer: FirJsr305StateContainer by FirSession.sessionComponentAccessor()
