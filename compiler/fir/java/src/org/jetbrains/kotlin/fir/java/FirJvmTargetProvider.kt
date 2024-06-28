/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent

class FirJvmTargetProvider(val jvmTarget: JvmTarget) : FirSessionComponent

val FirSession.jvmTargetProvider: FirJvmTargetProvider? by FirSession.nullableSessionComponentAccessor()
