// WITH_REFLECT
// FULL_JDK
// TARGET_BACKEND: JVM

// WITH_STDLIB

import java.lang.reflect.InvocationTargetException

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Simple(val x: String) {
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
