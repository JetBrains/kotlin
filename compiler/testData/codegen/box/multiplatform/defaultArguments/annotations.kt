// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: common.kt

expect annotation class A1(val x: Int, val y: String = "OK")

expect annotation class A2(val x: Int = 42, val y: String = "OK")

expect annotation class A3(val x: Int, val y: String)

expect annotation class A4(val x: Int = 42, val y: String)

@A1(0)
@A2
@A3
@A4
fun test() {}

// FILE: jvm.kt

import kotlin.test.assertEquals

actual annotation class A1(actual val x: Int, actual val y: String)

actual annotation class A2(actual val x: Int, actual val y: String = "OK")

actual annotation class A3(actual val x: Int = 42, actual val y: String = "OK")

actual annotation class A4(actual val x: Int, actual val y: String = "OK")

fun box(): String {
    val anno = Class.forName("CommonKt").getDeclaredMethod("test").annotations

    val a1 = anno.single { it.annotationClass == A1::class } as A1
    assertEquals(0, a1.x)
    assertEquals("OK", a1.y)

    val a2 = anno.single { it.annotationClass == A2::class } as A2
    assertEquals(42, a2.x)
    assertEquals("OK", a2.y)

    val a3 = anno.single { it.annotationClass == A3::class } as A3
    assertEquals(42, a3.x)
    assertEquals("OK", a3.y)

    val a4 = anno.single { it.annotationClass == A4::class } as A4
    assertEquals(42, a4.x)
    assertEquals("OK", a4.y)

    return "OK"
}
