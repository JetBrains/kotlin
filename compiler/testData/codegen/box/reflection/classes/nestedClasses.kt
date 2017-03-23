// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT
// FULL_JDK

import kotlin.reflect.KClass
import kotlin.reflect.jvm.*
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
    assertEquals(emptyList<String>(), nestedNames(A.Inner::class))
    // Kotlin class with nested classes
    assertEquals(listOf("Companion", "Inner", "Nested", "PrivateNested"), nestedNames(A::class))

    // Java class without nested classes
    assertEquals(emptyList<String>(), nestedNames(Error::class))
    // Java class with nested classes
    assertEquals(listOf("State", "UncaughtExceptionHandler"), nestedNames(Thread::class))

    // Built-ins
    assertEquals(emptyList<String>(), nestedNames(Array<Any>::class))
    assertEquals(emptyList<String>(), nestedNames(CharSequence::class))
    assertEquals(listOf("Companion"), nestedNames(String::class))

    assertEquals(emptyList<String>(), nestedNames(Collection::class))
    assertEquals(emptyList<String>(), nestedNames(MutableCollection::class))
    assertEquals(emptyList<String>(), nestedNames(List::class))
    assertEquals(emptyList<String>(), nestedNames(MutableList::class))
    assertEquals(listOf("Entry"), nestedNames(Map::class))
    assertEquals(emptyList<String>(), nestedNames(Map.Entry::class))
    assertEquals(emptyList<String>(), nestedNames(MutableMap.MutableEntry::class))

    // TODO: should be MutableEntry. Currently we do not distinguish between Map and MutableMap.
    assertEquals(listOf("Entry"), nestedNames(MutableMap::class))

    // Primitives
    for (primitive in listOf(Byte::class, Double::class, Float::class, Int::class, Long::class, Short::class, Char::class)) {
        assertEquals(listOf("Companion"), nestedNames(primitive))
    }
    assertEquals(emptyList<String>(), nestedNames(Boolean::class))

    // Primitive arrays
    for (primitiveArray in listOf(
            ByteArray::class, DoubleArray::class, FloatArray::class, IntArray::class,
            LongArray::class, ShortArray::class, CharArray::class, BooleanArray::class
    )) {
        assertEquals(emptyList<String>(), nestedNames(primitiveArray))
    }

    return "OK"
}
