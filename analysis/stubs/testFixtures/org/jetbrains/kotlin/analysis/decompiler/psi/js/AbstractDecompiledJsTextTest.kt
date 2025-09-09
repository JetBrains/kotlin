/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.js

import org.jetbrains.kotlin.analysis.decompiler.psi.AbstractDecompiledTextTest
import org.jetbrains.kotlin.platform.js.JsPlatforms

/**
 * Covers .knm files
 *
 * @see org.jetbrains.kotlin.analysis.stubs.js.AbstractCompiledJsStubsTest
 */
abstract class AbstractDecompiledJsTextTest : AbstractDecompiledTextTest(JsPlatforms.defaultJsPlatform)
