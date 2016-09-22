// WITH_REFLECT
// FILE: J.java

public class J {
    public static String x;

    static String packageLocalField;
}

// FILE: K.kt

import kotlin.test.assertEquals

fun box(): String {
    val f = J::x
    assertEquals("x", f.name)

    assertEquals(f, J::class.members.single { it.name == "x" })

    f.set("OK")
    assertEquals("OK", J.x)
    assertEquals("OK", f.getter())

    val pl = J::packageLocalField.getter
    try {
        pl()
        return "Fail: package local field must be inaccessible"
    } catch (e: Exception) {
        // OK
    }

    return f.get()
}
