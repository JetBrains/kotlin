/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

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
     * The Java classes declared by the `.java` sources of this compilation. Implementation decides the included sources.
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

object NoJavaInterop : FirJavaInterop {
    override fun createBinaryJavaFacade(
        session: FirSession,
        moduleData: FirModuleData,
        classpath: JvmClasspath,
    ): FirJavaFacade = shouldNotBeCalled("This compilation reads no Java")

    override fun createJavaSourcesFacade(
        session: FirSession,
        moduleData: FirModuleData,
    ): FirJavaFacade = shouldNotBeCalled("This compilation reads no Java")
}
