/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs.jvm

import org.jetbrains.kotlin.analysis.stubs.AbstractCompiledStubsTest
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

/**
 * Covers .class files
 *
 * @see org.jetbrains.kotlin.analysis.decompiler.psi.jvm.AbstractDecompiledJvmTextTest
 */
abstract class AbstractCompiledJvmStubsTest : AbstractCompiledStubsTest(JvmPlatforms.defaultJvmPlatform)
