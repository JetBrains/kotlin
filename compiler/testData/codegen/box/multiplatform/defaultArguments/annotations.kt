// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: JVM_IR, JS_IR
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: common.kt

expect annotation class A1(val x: Int, val y: String = "OK")

expect annotation class A2(val x: Int = 42, val y: String = "OK")

expect annotation class A3(val x: Int, val y: String)

expect annotation class A4(val x: Int = 42, val y: String)

expect annotation class A5()

expect annotation class A6()

@A1(0)
@A2
@A3
@A4
@A5
@A6
fun test() {}

// FILE: J1.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface J1 {
    String x() default "OK";
}

// FILE: jvm.kt

import kotlin.test.assertEquals

actual annotation class A1(actual val x: Int, actual val y: String)

actual annotation class A2(actual val x: Int, actual val y: String = "OK")

actual annotation class A3(actual val x: Int = 42, actual val y: String = "OK")

actual annotation class A4(actual val x: Int, actual val y: String = "OK")

actual annotation class A5(val x: String = "OK")

actual typealias A6 = J1

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

    val a5 = anno.single { it.annotationClass == A5::class } as A5
    assertEquals("OK", a5.x)

    val A6 = anno.single { it.annotationClass == A6::class } as A6
    assertEquals("OK", A6.x)

    return "OK"
}
