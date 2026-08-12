/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.jvm.environment.JvmClasspath

/**
 * Which Java implementation serves a compilation, in both directions: the two `create*JavaFacade` methods give
 * FIR the Java declarations of one compilation, [registerKotlinDeclarationsForJava] makes the Kotlin
 * declarations of a source session visible to Java resolution.
 *
 * The two directions are one object because they are one decision: an implementation which does not resolve
 * Java through PSI has no PSI Java resolution to feed either. The two peers are
 * `VfsBasedProjectEnvironment.psiJavaInterop()` (`:compiler:cli`) and `createJavaDirectJavaInterop`
 * (`:compiler:java-direct`); neither is a default, every consumer states its choice.
 */
interface FirJavaInterop {
    /**
     * The Java classes of [classpath], read from `.class` files.
     */
    fun createBinaryJavaFacade(
        session: FirSession,
        moduleData: FirModuleData,
        classpath: JvmClasspath,
    ): FirJavaFacade

    /**
     * The Java classes declared by the `.java` sources of this compilation. Which files those are is known to the
     * implementation, so there is no parameter: a caller cannot ask for the Java sources of something else.
     */
    fun createJavaSourcesFacade(
        session: FirSession,
        moduleData: FirModuleData,
    ): FirJavaFacade

    /**
     * Makes the Kotlin declarations of [session] visible to Java resolution, for an implementation which
     * resolves Java through PSI and therefore needs them as PSI stubs. A no-op otherwise.
     */
    fun registerKotlinDeclarationsForJava(session: FirSession) {}
}
