/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.java.direct.util.DefaultJavaSourceFileReader
import org.jetbrains.kotlin.java.direct.util.JavaSourceFileReader
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

internal class JavaDirectConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, JavaDirectPluginRegistrar())
    }
}

/**
 * Test-only [JavaClassFinderOverAstImpl] factory that supplies a dummy source-kind [FirSession].
 *
 * Production code wires a real [FirSession] in [JavaClassFinderOverAstFactory.createJavaClassFinder];
 * unit tests (`JavaParsingTest`-family, `JavaParsingClassFinderTest`, `JavaParsingLightweightScannerTest`)
 * exercise the model in isolation and do not have a fully-configured session at hand. They get a bare
 * [FirSession] with no registered components — sufficient as long as the model's resolution-time code
 * does not consult the symbol provider during parsing/index-population (the single invariant tracked by
 * `LazySessionAccess` once Step 4.5a's typed wrapper lands; see
 * `implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §7 mode 1).
 *
 * When a parsing test starts depending on resolution-time symbol-provider lookups, this helper should be
 * upgraded to the shared `JavaParsingTestFixture`-shaped session-builder described in §12 Q3 of that doc.
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
 * unit tests of the `java-direct` module. See KDoc on the [JavaClassFinderOverAstImpl] factory above.
 */
@OptIn(PrivateSessionConstructor::class)
internal fun createDummyFirSessionForTests(): FirSession =
    DummyJavaDirectFirSession(FirSession.Kind.Source)

@OptIn(PrivateSessionConstructor::class)
private class DummyJavaDirectFirSession(kind: Kind) : FirSession(kind)
