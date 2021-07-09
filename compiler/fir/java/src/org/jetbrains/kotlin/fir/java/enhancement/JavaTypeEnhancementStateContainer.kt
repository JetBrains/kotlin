/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState

class FirJavaTypeEnhancementStateComponent(val javaTypeEnhancementState: JavaTypeEnhancementState) : FirSessionComponent

private val FirSession.javaTypeEnhancementStateComponent: FirJavaTypeEnhancementStateComponent by FirSession.sessionComponentAccessor()

val FirSession.javaTypeEnhancementState: JavaTypeEnhancementState
    get() = javaTypeEnhancementStateComponent.javaTypeEnhancementState
