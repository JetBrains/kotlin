/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.collectProperties
import java.lang.reflect.Modifier
import kotlin.reflect.jvm.javaField

class CopyTests : TestCase() {
    fun testCollectPropertiesFiltersOutTransient() {
        val properties = collectProperties(CommonToolArguments::class, false)
        val errorsProperties = properties.single { it.name == "errors" }
        println(errorsProperties.annotations)
    }

    fun testCollectPropertiesDoesNotReturnTransient() {
        val errorProperty = CommonToolArguments::errors
        assertTrue(Modifier.isTransient(errorProperty.javaField!!.modifiers))

        val properties = collectProperties(CommonToolArguments::class, false)
        assertFalse(properties.any { it.name == errorProperty.name })
    }
}