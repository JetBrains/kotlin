// WITH_REFLECT
// TARGET_BACKEND: JVM
// FILE: B.java
public class B implements A {
    @Override
    public String f(String s) {
        return s;
    }
}

// FILE: box.kt
import kotlin.test.assertFails

interface A {
    fun f(s: String = "OK"): String
}

fun box(): String {
    // TODO(KT-86698): callBy fails for Java method with inherited default value
    assertFails {
        B::f.callBy(mapOf(B::f.parameters.first() to B()))
    }

    return "OK"
}
