/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.incremental.components.ModuleJavaClassesTracker
import java.io.File

abstract class FirJavaClassesTrackerComponent : FirSessionComponent {
    abstract fun report(javaClass: FirJavaClass, file: File?)
    abstract val tracker: ModuleJavaClassesTracker
}

val FirSession.javaClassesTracker: FirJavaClassesTrackerComponent? by FirSession.nullableSessionComponentAccessor()
