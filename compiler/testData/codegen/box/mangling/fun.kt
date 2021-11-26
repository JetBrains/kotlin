// TARGET_BACKEND: JVM

// WITH_STDLIB

package test

internal fun noMangling() = 1;

class Z {
    internal fun mangled() = 1;
}

fun box(): String {
    val clazz = Z::class.java
    val declaredMethods = clazz.declaredMethods

    val mangled = declaredMethods.firstOrNull {
        it.name.startsWith("mangled$")
    }
    if (mangled == null) return "Class internal function should exist"

    val topLevel = Class.forName("test.FunKt").getMethod("noMangling")
    if (topLevel == null) return "Top level internal function should exist"

    return "OK"
}
