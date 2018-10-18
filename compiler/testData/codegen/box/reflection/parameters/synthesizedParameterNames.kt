// SKIP_JDK6
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java

public class J {
    public J(String constructorParam) {}

    public void foo(int methodParam) {}

    public static void bar(J staticMethodParam) {}

    class Inner {
        public Inner(double innerParam, Object innerParam2) {}
    }
}

// FILE: K.kt

import kotlin.test.assertEquals

fun box(): String {
    assertEquals(listOf("arg0"), ::J.parameters.map { it.name })
    assertEquals(listOf(null, "arg0"), J::foo.parameters.map { it.name })
    assertEquals(listOf("arg0"), J::bar.parameters.map { it.name })
    assertEquals(listOf(null, "arg1", "arg2"), J::Inner.parameters.map { it.name })

    return "OK"
}
