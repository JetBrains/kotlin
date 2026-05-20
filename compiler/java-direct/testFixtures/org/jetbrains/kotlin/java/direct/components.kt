/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.java.direct.util.DefaultJavaSourceFileReader
import org.jetbrains.kotlin.java.direct.util.JavaSourceFileReader
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import kotlin.text.matches

/**
 * Enables `java-direct` for `JavaUsingAst*` tests by setting the
 * `JvmAnalysisFlags.useJavaDirect` analysis flag. `JvmFrontendPipelinePhase` consults this flag
 * to decide whether to wire `createJavaDirectSourceJavaFacadeBuilder` into the FIR session.
 *
 * Other tests using the same CLI test pipeline (Lombok, plain JVM black-box) leave the flag
 * unset → PSI-backed `FirJavaFacade`.
 */
internal class JavaDirectConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion,
    ): Map<AnalysisFlag<*>, Any?> = mapOf(JvmAnalysisFlags.useJavaDirect to true)
}

private val javaFileRegex = Regex("^\\s*//\\s* FILE:\\s* .*\\.java\\s*\$")

class OnlyTestsWithJavaSourcesMetaConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean =
        testServices.moduleStructure.originalTestDataFiles.first().useLines { lines -> lines.none { it.matches(javaFileRegex) } }
}

/**
 * Test-only [JavaClassFinderOverAstImpl] factory that supplies a dummy source-kind [FirSession].
 */
internal fun JavaClassFinderOverAstImpl(
    sourceRoots: List<VirtualFile>,
    sourceFileReader: JavaSourceFileReader = DefaultJavaSourceFileReader,
): JavaClassFinderOverAstImpl =
    JavaClassFinderOverAstImpl(
        createDummyFirSessionForTests(),
        JavaSourceRootEntry.fromRootsWithoutPrefix(sourceRoots),
        sourceFileReader,
    )

/**
 * Constructs a minimal [FirSession] with no registered components, intended only for parsing-level
 * unit tests of the `java-direct` module.
 */
@OptIn(PrivateSessionConstructor::class)
internal fun createDummyFirSessionForTests(): FirSession =
    DummyJavaDirectFirSession(FirSession.Kind.Source)

@OptIn(PrivateSessionConstructor::class)
private class DummyJavaDirectFirSession(kind: Kind) : FirSession(kind)
