/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// FILE: 1.kt

package codegen.annotations.annotations0

import kotlin.test.*
import kotlinx.serialization.*

@SerialInfo
annotation class Foo(val x: Int, val y: String)

@Test fun runTest() {
    val foo = @Suppress("ANNOTATION_CLASS_CONSTRUCTOR_CALL") Foo(42, "17")
    assertEquals(foo.x, 42)
    assertEquals(foo.y, "17")
}

// FILE: 2.kt

package kotlinx.serialization

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SerialInfo
