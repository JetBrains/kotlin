/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope

/**
 * Which Java implementation serves the Java declarations visible in a given search scope: it builds the
 * [FirJavaFacade] of a session, which in practice means choosing the `JavaClassFinder` inside it (PSI-based,
 * or the direct class-file/AST readers of `java-direct`).
 *
 * This is a property of the compilation, not of a single session, so it is held by
 * [FirJvmSessionFactory.Context] and every session created from that context takes it from there. Passing
 * it into each construction site instead is what let the choice drift: a site which does not know about it
 * silently keeps the default, as the symbol provider for the precompiled binaries of incremental
 * compilation did.
 */
fun interface FirJavaFacadeFactory {
    fun createJavaFacade(
        session: FirSession,
        moduleData: FirModuleData,
        fileSearchScope: AbstractProjectFileSearchScope,
    ): FirJavaFacade
}

/**
 * The PSI-backed Java view, and the default for every context which does not choose one; see
 * [AbstractProjectEnvironment.getFirJavaFacade].
 */
fun AbstractProjectEnvironment.psiJavaFacadeFactory(): FirJavaFacadeFactory =
    FirJavaFacadeFactory(this::getFirJavaFacade)
