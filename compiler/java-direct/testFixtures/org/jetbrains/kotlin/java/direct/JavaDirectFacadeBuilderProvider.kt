/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.deserialization.JvmBinaryClassFinderInputs
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.test.frontend.fir.JavaFacadeBuilderProvider
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Test-fixture wiring that mirrors the production CLI hook in
 * `JvmFrontendPipelinePhase.preprocessSessions`: returns a `javaFacadeBuilder` lambda backed by
 * `createJavaDirectSourceJavaFacadeBuilder`, so the `JavaUsingAst*TestGenerated` suites resolve
 * Java sources through `JavaClassFinderOverAstImpl` rather than PSI.
 */
class JavaDirectFacadeBuilderProvider(@Suppress("UNUSED_PARAMETER") testServices: TestServices) : JavaFacadeBuilderProvider() {
    override fun createBuilder(
        configuration: CompilerConfiguration,
        projectEnvironment: VfsBasedProjectEnvironment,
        librariesScope: AbstractProjectFileSearchScope,
    ): (AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade =
        createJavaDirectSourceJavaFacadeBuilder(configuration, projectEnvironment, librariesScope)

    /**
     * Stage 2 §6.3 (see `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`):
     * the deserializer-side companion. Returns a builder that produces a
     * [JvmBinaryClassFinderInputs] adapter backed by the same `BinaryJavaClassFinder` used by
     * the production CLI for the library scope, so `JvmClassFileBasedSymbolProvider` reads
     * binary `.class`/`.sig` files directly from the index instead of routing through
     * `FirJavaFacade`.
     */
    override fun createBinaryClassFinderInputsBuilder(
        configuration: CompilerConfiguration,
        projectEnvironment: VfsBasedProjectEnvironment,
    ): (AbstractProjectEnvironment, AbstractProjectFileSearchScope) -> JvmBinaryClassFinderInputs? =
        createJavaDirectBinaryClassFinderInputsBuilder(projectEnvironment)
}
