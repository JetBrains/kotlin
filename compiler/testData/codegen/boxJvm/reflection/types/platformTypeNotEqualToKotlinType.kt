// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public class J {
    public static String foo() {
        return "";
    }
}

// FILE: K.kt

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

fun nonNullString(): String = ""
fun nullableString(): String? = ""
fun platformString() = J.foo()

fun box(): String {
    assertNotEquals(J::foo.returnType, ::nonNullString.returnType)
    assertNotEquals(J::foo.returnType, ::nullableString.returnType)

    assertNotEquals(::nonNullString.returnType, ::platformString.returnType)
    assertNotEquals(::nullableString.returnType, ::platformString.returnType)

    assertEquals(J::foo.returnType, ::platformString.returnType)

    return "OK"
}
