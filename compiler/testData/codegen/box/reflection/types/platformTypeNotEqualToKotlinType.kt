// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public class J {
    public static String foo() {
        return "";
    }
}

// FILE: K.kt

import kotlin.test.assertNotEquals

fun nonNullString(): String = ""
fun nullableString(): String? = ""

fun box(): String {
    assertNotEquals(J::foo.returnType, ::nonNullString.returnType)
    assertNotEquals(J::foo.returnType, ::nullableString.returnType)

    return "OK"
}
