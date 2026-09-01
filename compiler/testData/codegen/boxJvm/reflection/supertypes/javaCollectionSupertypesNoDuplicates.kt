// TARGET_BACKEND: JVM
// WITH_REFLECT
// Tests that Java collection class supertypes do not contain duplicate entries.
// Each Kotlin-mapped supertype (MutableList, MutableSet, etc.) must appear at most once.

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

fun checkNoDuplicates(klass: kotlin.reflect.KClass<*>) {
    val supertypes = klass.supertypes
    val typeStrings = supertypes.map { it.toString() }
    val duplicates = typeStrings.groupBy { it }.filter { it.value.size > 1 }.keys
    assertTrue(duplicates.isEmpty(),
        "${klass.simpleName}::class.supertypes contains duplicates: $duplicates\nAll supertypes: $typeStrings")
}

fun box(): String {
    // LinkedList implements List + Deque; must not have MutableList twice
    checkNoDuplicates(java.util.LinkedList::class)

    // ArrayList also maps to MutableList
    checkNoDuplicates(java.util.ArrayList::class)

    // HashSet maps to MutableSet
    checkNoDuplicates(java.util.HashSet::class)

    // TreeSet maps to MutableSet + NavigableSet etc.
    checkNoDuplicates(java.util.TreeSet::class)

    // HashMap maps to MutableMap
    checkNoDuplicates(java.util.HashMap::class)

    // Verify the specific LinkedList count: MutableList must appear exactly once
    val linkedListSupertypes = java.util.LinkedList::class.supertypes.map { it.toString() }
    val mutableListCount = linkedListSupertypes.count { it.contains("MutableList") }
    assertEquals(1, mutableListCount,
        "LinkedList supertypes must contain MutableList exactly once, got: $linkedListSupertypes")

    return "OK"
}
