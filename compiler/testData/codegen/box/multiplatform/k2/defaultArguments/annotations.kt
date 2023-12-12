// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect annotation class A1(val x: Int, val y: String = "OK")

expect annotation class A2(val x: Int = 42, val y: String = "OK")

@A1(0)
@A2
fun test() {}

// MODULE: jvm()()(common)
// FILE: jvm.kt

import kotlin.test.assertEquals

actual annotation class A1(actual val x: Int, actual val y: String)

actual annotation class A2(actual val x: Int, actual val y: String = "OK")

fun box(): String {
    val anno = Class.forName("CommonKt").getDeclaredMethod("test").annotations

    val a1 = anno.single { it.annotationClass == A1::class } as A1
    assertEquals(0, a1.x)
    assertEquals("OK", a1.y)

    val a2 = anno.single { it.annotationClass == A2::class } as A2
    assertEquals(42, a2.x)
    assertEquals("OK", a2.y)

    return "OK"
}
