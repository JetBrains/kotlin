// IGNORE_BACKEND_FIR: JVM_IR
// WITH_REFLECT
// FULL_JDK
// TARGET_BACKEND: JVM

import java.lang.reflect.InvocationTargetException

inline class Simple(val x: String) {
    fun somethingWeird() {}
}

fun box(): String {
    var s = ""
    val name = "equals-impl0"
    val specializedEquals =
        Simple::class.java.getDeclaredMethod(name, String::class.java, String::class.java)
            ?: return "$name not found"

    if (specializedEquals.invoke(null, "a", "b") as Boolean)
        return "Fail"
    return "OK"
}
