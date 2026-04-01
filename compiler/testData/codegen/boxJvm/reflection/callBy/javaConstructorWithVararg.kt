// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java

public class J {
    public final String result;

    public J(String... s) {
        this.result = s[0] + s[1];
    }
    public J(int x, byte... s) {
        this.result = String.valueOf(x);
    }
}

// FILE: K.kt

import kotlin.reflect.full.*
import kotlin.reflect.jvm.*
import kotlin.test.*

fun box(): String {
    val a = J::class.constructors.single { it.parameters.size == 2 }
    assertEquals("42", a.callBy(mapOf(a.parameters.first() to 42)).result)

    val c = J::class.constructors.single { it.parameters.size == 1 }
    return c.callBy(mapOf(c.parameters.single() to arrayOf("O", "K"))).result
}
