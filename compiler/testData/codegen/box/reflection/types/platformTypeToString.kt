// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

import java.util.List;

public class J {
    public static String string() {
        return "";
    }

    public static List<Object> list() {
        return null;
    }
}

// FILE: K.kt

import kotlin.test.assertEquals

fun box(): String {
    assertEquals("kotlin.String!", J::string.returnType.toString())
    assertEquals("kotlin.collections.(Mutable)List<kotlin.Any!>!", J::list.returnType.toString())

    return "OK"
}
