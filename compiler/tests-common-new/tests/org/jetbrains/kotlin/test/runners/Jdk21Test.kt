/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.junit.jupiter.api.Tag

/**
 * This tag is only necessary while JDK 21 is not released, so cannot be obtained via toolchain.
 * See KT-58765 for tracking
 */
@Tag("Jdk21Test")
annotation class Jdk21Test
