// TARGET_BACKEND: JVM
// WITH_REFLECT
// Tests that the parameter of `compareTo` inherited by a Kotlin enum from Enum<E> has E substituted with the enum type in javaType.
// See javaInheritedSubstitutedMethods.kt for the similar check of `getDeclaringClass` on a Java enum.

import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

enum class Season { SPRING, SUMMER }

enum class Weekday {
    MON {
        override fun next(): Weekday = TUE
    },
    TUE {
        override fun next(): Weekday = MON
    };

    abstract fun next(): Weekday
}

private val useK1 = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true

private fun check(enumClass: KClass<out Enum<*>>) {
    val compareTo = enumClass.members.single { it.name == "compareTo" }
    if (useK1) {
        // KT-87366
        assertEquals("E", compareTo.parameters[1].type.javaType.toString())
    } else {
        assertEquals(enumClass.java, compareTo.parameters[1].type.javaType)
    }
}

fun box(): String {
    check(Season::class)
    // Entries with bodies are anonymous subclasses, but the members must still be substituted with the enum class itself.
    check(Weekday::class)
    return "OK"
}
