// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt

package test

import a.A
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

fun box(): String {
    assertEquals("(a.A<(a.A<a.A<*, *>!, a.A<*, *>!>..a.A<*, *>?), (a.A<a.A<*, *>!, a.A<*, *>!>..a.A<*, *>?)>..a.A<*, *>?)", returnTypeOf { A.rawRecursive() }.toString())

    return "OK"
}

inline fun <reified T : Any> returnTypeOf(block: () -> T) =
    typeOf<T>()

// FILE: a/A.java

package a;

public class A<T extends A, F extends T> {
    public static A rawRecursive() {
        return null;
    }
}
