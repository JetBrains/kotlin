// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: P4c.java
public abstract class P4c implements P4i1, P4i2 {
}

// FILE: box.kt
import kotlin.test.assertEquals

interface P4i1 {
    fun f(x: List<String>): CharSequence
}

interface P4i2 {
    fun f(x: MutableList<String>): CharSequence
}

fun box(): String {
    assertEquals(
        listOf("kotlin.collections.List<kotlin.String>", "kotlin.collections.MutableList<kotlin.String>"),
        P4c::class.members.filter { it.name == "f" }.map { it.parameters[1].type.toString() }.sorted(),
    )
    return "OK"
}
