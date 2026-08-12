/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.search.AbstractProjectFileSearchScope

/**
 * Which Java implementation serves a compilation, in both directions: [createJavaFacade] gives FIR the Java
 * declarations of a scope, [registerKotlinDeclarationsForJava] makes the Kotlin declarations of a source
 * session visible to Java resolution.
 *
 * The two directions are one object because they are one decision: an implementation which does not resolve
 * Java through PSI has no PSI Java resolution to feed either. The two peers are
 * `VfsBasedProjectEnvironment.psiJavaInterop()` (`:compiler:cli`) and `createJavaDirectJavaInterop`
 * (`:compiler:java-direct`); neither is a default, every consumer states its choice.
 */
interface FirJavaInterop {
    fun createJavaFacade(
        session: FirSession,
        moduleData: FirModuleData,
        fileSearchScope: AbstractProjectFileSearchScope,
    ): FirJavaFacade

    /**
     * Makes the Kotlin declarations of [session] visible to Java resolution, for an implementation which
     * resolves Java through PSI and therefore needs them as PSI stubs. A no-op otherwise.
     */
    fun registerKotlinDeclarationsForJava(session: FirSession) {}
}
