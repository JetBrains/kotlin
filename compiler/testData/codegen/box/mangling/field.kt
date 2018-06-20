// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

package test

internal val noMangling = 1;

class Z {
    internal var noMangling = 1;
}

fun box(): String {
    val clazz = Z::class.java
    val classField = clazz.getDeclaredField("noMangling")
    if (classField == null) return "Class internal backing field should exist"

    val topLevel = Class.forName("test.FieldKt").getDeclaredField("noMangling")
    if (topLevel == null) return "Top level internal backing field should exist"

    return "OK"
}
