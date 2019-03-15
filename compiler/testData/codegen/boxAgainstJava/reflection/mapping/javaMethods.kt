// IGNORE_BACKEND: JVM_IR
// WITH_REFLECT
// FILE: J.java

public class J {
    public String f(String s) {
        return s;
    }

    public static String g(String s) {
        return s;
    }
}

// FILE: 1.kt

import kotlin.reflect.*
import kotlin.reflect.jvm.*

fun box(): String {
    val f = J::f
    val fm = f.javaMethod ?: return "Fail: no Method for f"
    if (fm.invoke(J(), "abc") != "abc") return "Fail fm"
    val ff = fm.kotlinFunction ?: return "Fail: no KFunction for fm"
    if (f != ff) return "Fail f != ff"

    val g = J::g
    val gm = g.javaMethod ?: return "Fail: no Method for g"
    if (gm.invoke(null, "ghi") != "ghi") return "Fail gm"
    val gg = gm.kotlinFunction ?: return "Fail: no KFunction for gm"
    if (g != gg) return "Fail g != gg"

    return "OK"
}
