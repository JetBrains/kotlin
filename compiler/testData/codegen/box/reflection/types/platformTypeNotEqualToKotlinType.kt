// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
