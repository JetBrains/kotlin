/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.arguments.ArgumentParseErrors
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyBeanTo
import java.lang.reflect.Modifier
import kotlin.reflect.jvm.javaField

class ArgumentUtilTest : TestCase() {
    fun testCopyDoesNotCopyTransientFields() {
        assertTrue(Modifier.isTransient(K2JVMCompilerArguments::errors.javaField!!.modifiers))

        val a = K2JVMCompilerArguments()
        a.errors = ArgumentParseErrors()
        a.moduleName = "my module name"

        val b = K2JVMCompilerArguments()
        assertNull(b.errors)
        assertNull(b.moduleName)

        copyBeanTo(a, b)

        assertNull(b.errors)
        assertEquals("my module name", b.moduleName)
    }
}