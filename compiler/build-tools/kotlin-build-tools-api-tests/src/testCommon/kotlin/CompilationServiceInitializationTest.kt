/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URLClassLoader

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class CompilationServiceInitializationTest {
    @Test
    fun noImplementationProvided() {
        val e = assertThrows<IllegalStateException> {
            CompilationService.loadImplementation(URLClassLoader(emptyArray(), null))
        }
        assertEquals("The classpath contains no implementation for org.jetbrains.kotlin.buildtools.api.CompilationService", e.message)
    }

    @Test
    fun implementationProvided() {
        CompilationService.loadImplementation(CompilationServiceInitializationTest::class.java.classLoader) // implementation is in the runtime classpath
    }
}