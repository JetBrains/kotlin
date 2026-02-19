// KT-66094: java.lang.InstantiationError: Foo
// WITH_STDLIB

// FILE: 1.kt

import kotlin.test.*

@SerialInfo
annotation class Foo(val x: Int, val y: String)

fun box(): String {
    val foo = @Suppress("ANNOTATION_CLASS_CONSTRUCTOR_CALL") Foo(42, "OK")
    assertEquals(foo.x, 42)
    return foo.y
}

// FILE: 2.kt

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SerialInfo
