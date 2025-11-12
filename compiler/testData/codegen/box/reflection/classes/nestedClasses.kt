// TARGET_BACKEND: JVM

// WITH_REFLECT
// FULL_JDK

import kotlin.reflect.KClass
import kotlin.test.assertEquals

class A {
    companion object {}
    inner class Inner
    class Nested
    private class PrivateNested
}

fun nestedNames(c: KClass<*>) = c.nestedClasses.map { it.simpleName ?: throw AssertionError("Unnamed class: ${it.java}") }.sorted()

fun box(): String {
    // Kotlin class without nested classes
    assertEquals(listOf(), nestedNames(A.Inner::class))
    // Kotlin class with nested classes
    assertEquals(listOf("Companion", "Inner", "Nested", "PrivateNested"), nestedNames(A::class))

    // Java class without nested classes
    assertEquals(listOf(), nestedNames(Error::class))
    // Java interface with nested classes
    assertEquals(listOf("Entry"), nestedNames(java.util.Map::class))
    // Java class with nested classes
    assertEquals(listOf("SimpleEntry", "SimpleImmutableEntry"), nestedNames(java.util.AbstractMap::class) - "ViewCollection" - "KeyIterator" - "ValueIterator")

    // Built-ins
    assertEquals(listOf("Companion"), nestedNames(Array<Any>::class))
    assertEquals(listOf(), nestedNames(CharSequence::class))
    assertEquals(listOf("Companion"), nestedNames(String::class))

    assertEquals(listOf(), nestedNames(Collection::class))
    assertEquals(listOf(), nestedNames(MutableCollection::class))
    assertEquals(listOf("Companion"), nestedNames(List::class))
    assertEquals(listOf("Companion"), nestedNames(MutableList::class))
    assertEquals(listOf("Entry"), nestedNames(Map::class))
    assertEquals(listOf(), nestedNames(Map.Entry::class))
    assertEquals(listOf(), nestedNames(MutableMap.MutableEntry::class))

    // TODO: should be MutableEntry. Currently we do not distinguish between Map and MutableMap.
    assertEquals(listOf("Entry"), nestedNames(MutableMap::class))

    // Primitives
    for (primitive in listOf(Byte::class, Double::class, Float::class, Int::class, Long::class, Short::class, Char::class, Boolean::class)) {
        assertEquals(listOf("Companion"), nestedNames(primitive))
    }

    // Primitive arrays
    for (primitiveArray in listOf(
        ByteArray::class, DoubleArray::class, FloatArray::class, IntArray::class,
        LongArray::class, ShortArray::class, CharArray::class, BooleanArray::class
    )) {
        assertEquals(listOf("Companion"), nestedNames(primitiveArray))
    }

    return "OK"
}
