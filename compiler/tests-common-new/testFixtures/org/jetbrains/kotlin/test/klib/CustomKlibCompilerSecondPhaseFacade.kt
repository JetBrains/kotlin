/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices

/**
 * This is a test facade created specifically for running KLIB forward-compatibility tests.
 * The goal of such tests it to guarantee the ability for the older released versions of the compiler
 * to read KLIBs produced by the new compiler.
 *
 * See also https://kotlinlang.org/docs/kotlin-evolution-principles.html#kotlin-klib-binaries
 *
 * Important: It is assumed that the older compiler supports the metadata version and the ABI version
 * which are used in generated KLIBs.
 *
 * The sources are compiler by the new (current) compiler. Then all produced KLIBs are passed
 * to the old (custom) compiler using an implementation of [CustomKlibCompilerSecondPhaseFacade]
 * to produce the executable binary. Finally, the binary is executed and the execution result is verified.
 *
 * This facade effectively replaces a few smaller facades, and allows directly transforming
 * [BinaryArtifacts.KLib] artifacts to the corresponding platform-specific binary artifacts.
 */
abstract class CustomKlibCompilerSecondPhaseFacade<A : ResultingArtifact.Binary<A>>(
    val testServices: TestServices,
) : AbstractTestFacade<BinaryArtifacts.KLib, A>() {

    final override val inputKind get() = ArtifactKinds.KLib

    final override fun shouldTransform(module: TestModule) = isMainModule(module)

    final override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): A {
        val (regularDependencies: Set<String>, friendDependencies: Set<String>) = collectDependencies(module)

        return compileBinary(
            module = module,
            customArgs = emptyList(),
            mainLibrary = inputArtifact.outputFile.absolutePath,
            regularDependencies = regularDependencies,
            friendDependencies = friendDependencies
        )
    }

    protected abstract fun isMainModule(module: TestModule): Boolean

    /** Returns the set of regular dependency paths and the set of friend dependency paths. */
    protected abstract fun collectDependencies(module: TestModule): Pair<Set<String>, Set<String>>

    /** Invokes a custom KLIB compiler. */
    protected abstract fun compileBinary(
        module: TestModule,
        customArgs: List<String>,
        mainLibrary: String,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ): A
}
