// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public class J {
    private String result = "Fail";
}

// FILE: K.kt

import kotlin.reflect.*
import kotlin.reflect.jvm.*

fun box(): String {
    val a = J()
    val p = J::class.members.single { it.name == "result" } as KMutableProperty1<J, String>
    p.isAccessible = true
    p.set(a, "OK")
    return p.get(a)
}
