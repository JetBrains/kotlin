/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.junit.jupiter.api.Tag

/**
 * Default mode for FIR compiler uses light tree, so all regular tests should specify it
 * To ensure that PSI mode works as well each codegen test should have PSI counterpart
 *   with this annotation.
 *  Annotation excludes test class from regular aggregate test suite and adds it to nightly build
 */
@Tag("FirPsiCodegenTest")
annotation class FirPsiCodegenTest
