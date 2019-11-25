// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

@Anno("J")
public class J {
    @Anno("foo")
    public static int foo = 42;

    @Anno("bar")
    public static void bar() {}

    @Anno("constructor")
    public J() {}
}

// FILE: K.kt

import kotlin.test.assertEquals
import kotlin.reflect.KAnnotatedElement

annotation class Anno(val value: String)

fun box(): String {
    assertEquals("J", getSingleAnnoAnnotation(J::class).value)
    assertEquals("foo", getSingleAnnoAnnotation(J::foo).value)
    assertEquals("bar", getSingleAnnoAnnotation(J::bar).value)
    assertEquals("constructor", getSingleAnnoAnnotation(::J).value)

    return "OK"
}

fun getSingleAnnoAnnotation(annotated: KAnnotatedElement): Anno = annotated.annotations.single() as Anno