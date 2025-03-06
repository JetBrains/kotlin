/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent

class FirJvmDefaultModeComponent(val jvmDefaultMode: JvmDefaultMode) : FirSessionComponent

private val FirSession.jvmDefaultModeComponent: FirJvmDefaultModeComponent by FirSession.sessionComponentAccessor()

val FirSession.jvmDefaultModeState: JvmDefaultMode
    get() = jvmDefaultModeComponent.jvmDefaultMode
