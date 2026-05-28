/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Test-fixture hook for plugging an alternative `JavaSymbolProvider` / `JvmClassFileBasedSymbolProvider`
 * `FirJavaFacade` builder into the FIR JVM session via
 * `FirJvmSessionFactory.Context.javaFacadeBuilder`. The CLI plugs `java-direct` through this seam
 * in production (`JvmFrontendPipelinePhase.createJavaDirectSourceJavaFacadeBuilder`); tests do the
 * same by registering an implementation of this service.
 *
 * Returns `null` from [createBuilder] to opt out, in which case the FIR session falls back to
 * `projectEnvironment.getFirJavaFacade(...)`.
 */
abstract class JavaFacadeBuilderProvider : TestService {
    /**
     * Stage 2 §6.4 (see `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`):
     * [librariesScope] is threaded through so a java-direct builder can identity-compare it
     * against the per-call `scope` argument of the returned lambda and route source vs library
     * sessions to different facades (source-only `JavaClassFinderOverAstImpl` vs binary-only
     * `BinaryJavaClassFinder`).
     */
    abstract fun createBuilder(
        configuration: CompilerConfiguration,
        projectEnvironment: VfsBasedProjectEnvironment,
        librariesScope: AbstractProjectFileSearchScope,
    ): ((AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade)?
}

val TestServices.javaFacadeBuilderProvider: JavaFacadeBuilderProvider? by TestServices.nullableTestServiceAccessor()
