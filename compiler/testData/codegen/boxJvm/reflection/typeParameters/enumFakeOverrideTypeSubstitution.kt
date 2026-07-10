// TARGET_BACKEND: JVM
// WITH_REFLECT
// Tests that fake overrides inherited by a Kotlin enum from Enum<E> have E substituted with the enum type in javaType.
// See javaInheritedSubstitutedMethods.kt for the same check on a Java enum.

import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

enum class Season { SPRING, SUMMER }

enum class Weekday(val abbreviation: String) {
    MON("Mon") {
        override fun next(): Weekday = TUE
    },
    TUE("Tue") {
        override fun next(): Weekday = MON
    };

    abstract fun next(): Weekday
}

private val useK1 = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true

private fun check(enumClass: KClass<out Enum<*>>) {
    val getDeclaringClass = enumClass.members.single { it.name == "getDeclaringClass" }
    val compareTo = enumClass.members.single { it.name == "compareTo" }
    if (useK1) {
        // KT-87366
        assertEquals("java.lang.Class<E>", getDeclaringClass.returnType.javaType.toString())
        assertEquals("E", compareTo.parameters[1].type.javaType.toString())
    } else {
        assertEquals("java.lang.Class<${enumClass.java.name}>", getDeclaringClass.returnType.javaType.toString())
        assertEquals(enumClass.java, compareTo.parameters[1].type.javaType)
    }
    assertEquals("kotlin.Int", compareTo.returnType.toString())
}

fun box(): String {
    check(Season::class)
    // Entries with bodies are anonymous subclasses, but the members must still be substituted with the enum class itself.
    check(Weekday::class)
    assertEquals(Weekday::class.java, Weekday::class.members.single { it.name == "getDeclaringClass" }.call(Weekday.MON))
    return "OK"
}
