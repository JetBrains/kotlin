// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT
// FILE: J.java

public class J {
    private static String result = "Fail";
}

// FILE: K.kt

import kotlin.reflect.*
import kotlin.reflect.jvm.*

fun box(): String {
    val a = J()
    val p = J::class.members.single { it.name == "result" } as KMutableProperty0<String>
    p.isAccessible = true
    p.set("OK")
    return p.get()
}
