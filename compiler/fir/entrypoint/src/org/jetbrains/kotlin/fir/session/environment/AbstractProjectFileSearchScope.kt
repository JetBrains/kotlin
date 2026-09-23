/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session.environment

/**
 * Used in `IncrementalCompilationComponentsWithCustomScope` for compatibility with the current IJ/Bazel code. (see KT-88475).
 * Currently preferred variant is to use `VirtualJvmClasspathRoot.isPrecompiledOutput` instead.
 */
@Deprecated("Transitional: mark the previous build's output with VirtualJvmClasspathRoot.isPrecompiledOutput instead")
interface AbstractProjectFileSearchScope
