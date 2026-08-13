/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import java.nio.file.Path

/**
 * Custom incremental compilation components (the IntelliJ build system has such an implementation) which know
 * where the previous build's output is, instead of it being `CompilerConfiguration.outputDirectory`.
 *
 * The successor of the former `IncrementalCompilationComponentsWithCustomScope.createSearchScope`, which
 * returned an IntelliJ-shaped file set: a part of the classpath is now described by its roots
 * ([org.jetbrains.kotlin.jvm.environment.JvmClasspath]), so this hook says the same thing in that currency and
 * needs neither a project environment nor PSI. The roots are the ones the previous build wrote to, exactly as
 * they would be spelled on a classpath — a directory, or a `.jar`.
 *
 * They are used for two things at once, both in
 * [prepareIncrementalCompilationContextAndLibrariesClasspath]: they are the classpath of the symbol providers
 * for precompiled binaries, and they are excluded from the regular classpath of this compilation, so that the
 * same class files are not served by both.
 */
interface IncrementalCompilationComponentsWithCustomPrecompiledBinaries : IncrementalCompilationComponents {
    fun precompiledBinariesRoots(): List<Path>
}
