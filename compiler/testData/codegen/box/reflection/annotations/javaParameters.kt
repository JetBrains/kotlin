// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java

public class J {
    public static void staticMethod(@Anno("static") String s, int x) {}

    public void instanceMethod(int x, @Anno("instance") String s) {}

    public static String staticField = "";
    public String instanceField = "";
}

// FILE: K.kt

import kotlin.test.assertEquals
import kotlin.reflect.*

annotation class Anno(val value: String)

private fun getParameterAnnotations(callable: KCallable<*>): String =
    callable.parameters.joinToString { p ->
        p.annotations.joinToString(prefix = "[", postfix = "]") { (it as Anno).value }
    }

fun box(): String {
    assertEquals("[static], []", getParameterAnnotations(J::staticMethod))
    assertEquals("[], [], [instance]", getParameterAnnotations(J::instanceMethod))
    assertEquals("", getParameterAnnotations(J::staticField))
    assertEquals("[]", getParameterAnnotations(J::instanceField))

    return "OK"
}
