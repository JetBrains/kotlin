/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.arguments.*
import java.lang.reflect.Modifier
import kotlin.reflect.jvm.javaField
import kotlin.test.assertContentEquals

class GenerateCompilerArgumentsCopyTest : TestCase() {
    fun testCompilerArgumentsCopyFunctionsAreUpToDate() {
        generateCompilerArgumentsCopy(::getPrinterForTests)
    }

    fun testCopyDoesNotCopyTransientFields() {
        val a = K2JVMCompilerArguments()
        a.errors = ArgumentParseErrors()
        a.moduleName = "my module name"

        val b = K2JVMCompilerArguments()
        assertNull(b.errors)
        assertNull(b.moduleName)

        copyK2JVMCompilerArguments(a, b)
        assertNull(b.errors)
        assertEquals("my module name", b.moduleName)
    }

    fun testCopyDuplicatesArray() {
        val a = K2JVMCompilerArguments()
        a.additionalJavaModules = arrayOf("xxx")

        val b = K2JVMCompilerArguments()
        copyK2JVMCompilerArguments(a, b)

        assertContentEquals(a.additionalJavaModules, b.additionalJavaModules)
        assertNotSame(a.additionalJavaModules, b.additionalJavaModules)

        b.additionalJavaModules!![0] = "yyy"
        assertEquals("xxx", a.additionalJavaModules!![0])
    }

    fun testCollectPropertiesDoesNotReturnTransient() {
        val errorProperty = CommonToolArguments::errors
        assertTrue(Modifier.isTransient(errorProperty.javaField!!.modifiers))

        val properties = collectProperties(CommonToolArguments::class, false)
        assertFalse(properties.any { it.name == errorProperty.name })
    }
}