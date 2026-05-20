/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.java.direct.util.DefaultJavaSourceFileReader
import org.jetbrains.kotlin.java.direct.util.JavaSourceFileReader
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import kotlin.text.matches

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
