// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java

public class J {
    static String packagePrivateField;
}

// FILE: K.kt

import kotlin.reflect.full.IllegalCallableAccessException
import kotlin.test.assertFailsWith

fun box(): String {
    val f = J::packagePrivateField

    val getter = f.getter
    assertFailsWith(IllegalCallableAccessException::class) { getter() }
    val setter = f.setter!!
    assertFailsWith(IllegalCallableAccessException::class) { setter("") }

    return "OK"
}
