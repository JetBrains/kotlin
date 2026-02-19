// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect annotation class A1(val x: Int, val y: String = "OK")

expect annotation class A2(val x: Int = 42, val y: String = "OK")

expect annotation class A3()

expect annotation class A4()

@A1(0)
@A2
@A3
@A4
fun test() {}

// MODULE: jvm()()(common)
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

actual annotation class A3(val x: String = "OK")

actual typealias A4 = J1

fun box(): String {
    val anno = Class.forName("CommonKt").getDeclaredMethod("test").annotations

    val a1 = anno.single { it.annotationClass == A1::class } as A1
    assertEquals(0, a1.x)
    assertEquals("OK", a1.y)

    val a2 = anno.single { it.annotationClass == A2::class } as A2
    assertEquals(42, a2.x)
    assertEquals("OK", a2.y)

    val a3 = anno.single { it.annotationClass == A3::class } as A3
    assertEquals("OK", a3.x)

    val a4 = anno.single { it.annotationClass == A4::class } as A4
    assertEquals("OK", a4.x)

    return "OK"
}
