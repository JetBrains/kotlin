/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass

abstract class FirJavaClassesTrackerComponent : FirSessionComponent {
    abstract fun report(javaClass: FirJavaClass, file: FirFile?)
}

val FirSession.javaClassesTracker: FirJavaClassesTrackerComponent? by FirSession.nullableSessionComponentAccessor()
