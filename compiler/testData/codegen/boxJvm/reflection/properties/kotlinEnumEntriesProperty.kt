// TARGET_BACKEND: JVM
// WITH_REFLECT

// Kotlin counterpart of javaEnumEntriesProperty.kt.

package test

import kotlin.reflect.KProperty0
import kotlin.test.assertEquals
import kotlin.test.assertFails

enum class Color { RED, GREEN, BLUE }

private val systemProperties = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt")
private val useK1ForMembers = systemProperties.getMethod("getUseK1Implementation").invoke(null) == true ||
        systemProperties.getMethod("getUseK1ImplementationForMembers").invoke(null) == true

fun box(): String {
    val entries = Color::class.members.single { it.name == "entries" } as KProperty0<*>
    assertEquals("val entries: kotlin.enums.EnumEntries<test.Color>", entries.toString())
    assertEquals(Color::entries, entries)
    assertEquals(entries, Color::entries)
    assertEquals(Color::entries.hashCode(), entries.hashCode())

    val checkCalls = {
        assertEquals(Color.entries, entries.get())
        assertEquals(Color.entries, entries.call())
        assertEquals(Color.entries, entries.callBy(emptyMap()))
        assertEquals(Color.entries, entries.getter.call())
    }
    if (useK1ForMembers) {
        // Calls of Kotlin enum `entries` are not supported in K1-based reflection.
        assertFails { checkCalls() }
    } else {
        checkCalls()
    }

    return "OK"
}
