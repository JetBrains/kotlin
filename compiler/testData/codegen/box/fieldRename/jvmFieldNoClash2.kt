// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

import java.lang.reflect.Modifier

class A {
    val x = "outer"
    val y = "outer"

    companion object {
        @JvmField
        val x = "companion"

        const val y = "companion"
    }
}

fun box(): String {
    if (A().x != "outer") return "Fail outer x"
    if (A().y != "outer") return "Fail outer y"
    if (A.x != "companion") return "Fail companion x"
    if (A.y != "companion") return "Fail companion y"

    if (!Modifier.isStatic(A::class.java.getDeclaredField("x").modifiers))
        return "Fail: A.x should be static"

    if (!Modifier.isStatic(A::class.java.getDeclaredField("y").modifiers))
        return "Fail: A.y should be static"

    if (Modifier.isStatic(A::class.java.getDeclaredField("x$1").modifiers))
        return "Fail: A.x$1 should not be static"

    if (Modifier.isStatic(A::class.java.getDeclaredField("y$1").modifiers))
        return "Fail: A.y$1 should not be static"

    return "OK"
}
