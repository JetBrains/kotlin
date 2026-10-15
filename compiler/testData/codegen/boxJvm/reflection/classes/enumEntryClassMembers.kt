// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt
import kotlin.reflect.full.declaredMembers
import kotlin.test.assertEquals

enum class E {
    A {
        val extra = 1
        override fun f() = 1
    };
    abstract fun f(): Int
}

fun box(): String {
    val disallowedNames = listOf("getExtra", "values", "valueOf", "entries")
    assertEquals(emptyList<String>(), E.A::class.members.map { it.name }.filter { it in disallowedNames })

    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) != true) {
        assertEquals(setOf("extra", "f"), E.A::class.declaredMembers.map { it.name }.toSet())
    }

    return "OK"
}
