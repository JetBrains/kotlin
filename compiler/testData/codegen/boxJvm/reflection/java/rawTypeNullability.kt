// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: test/J.java
package test;

import java.util.List;

public class J<T> {
    public static J j() { return null; }
    public static List list() { return null; }
}

// FILE: box.kt
import kotlin.test.assertEquals
import test.J

fun box(): String {
    assertEquals("test.J<(raw) kotlin.Any!>!", J<*>::j.returnType.toString())
    assertEquals("kotlin.collections.(Mutable)List<(raw) kotlin.Any?>!", J<*>::list.returnType.toString())
    return "OK"
}
