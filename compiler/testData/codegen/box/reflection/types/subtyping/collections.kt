// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface A {
    fun iterable(): Iterable<*>
    fun collection(): Collection<*>
    fun list(): List<*>
    fun iterator(): Iterator<*>
    fun listIterator(): ListIterator<*>
    fun map(): Map<*, *>
    fun mapEntry(): Map.Entry<*, *>
    fun set(): Set<*>

    fun mutableIterable(): MutableIterable<*>
    fun mutableCollection(): MutableCollection<*>
    fun mutableList(): MutableList<*>
    fun mutableIterator(): MutableIterator<*>
    fun mutableListIterator(): MutableListIterator<*>
    fun mutableMap(): MutableMap<*, *>
    fun mutableMapEntry(): MutableMap.MutableEntry<*, *>
    fun mutableSet(): MutableSet<*>
}

fun checkSubtype(subtype: KCallable<*>, supertype: KCallable<*>) {
    assertTrue(subtype.returnType.isSubtypeOf(supertype.returnType), "Expected $subtype to be a subtype of $supertype")
    assertTrue(supertype.returnType.isSupertypeOf(subtype.returnType), "Expected $subtype to be a subtype of $supertype")
}

fun checkNotSubtype(subtype: KCallable<*>, supertype: KCallable<*>) {
    assertFalse(subtype.returnType.isSubtypeOf(supertype.returnType), "Expected $subtype NOT to be a subtype of $supertype")
}

fun checkStrictSubtype(subtype: KCallable<*>, supertype: KCallable<*>) {
    checkSubtype(subtype, supertype)
    checkNotSubtype(supertype, subtype)
}

fun box(): String {
    checkStrictSubtype(A::collection, A::iterable)
    checkStrictSubtype(A::list, A::collection)
    checkStrictSubtype(A::list, A::iterable)
    checkStrictSubtype(A::list, A::iterable)
    checkStrictSubtype(A::listIterator, A::iterator)
    checkStrictSubtype(A::set, A::collection)
    checkStrictSubtype(A::set, A::iterable)

    checkNotSubtype(A::iterator, A::iterable)
    checkNotSubtype(A::map, A::iterable)
    checkNotSubtype(A::map, A::collection)
    checkNotSubtype(A::mapEntry, A::iterable)
    checkNotSubtype(A::mapEntry, A::map)

    checkStrictSubtype(A::mutableIterable, A::iterable)
    checkStrictSubtype(A::mutableCollection, A::iterable)
    checkStrictSubtype(A::mutableCollection, A::mutableIterable)
    checkStrictSubtype(A::mutableCollection, A::collection)
    checkStrictSubtype(A::mutableList, A::iterable)
    checkStrictSubtype(A::mutableList, A::mutableIterable)
    checkStrictSubtype(A::mutableList, A::collection)
    checkStrictSubtype(A::mutableList, A::mutableCollection)
    checkStrictSubtype(A::mutableList, A::list)
    checkStrictSubtype(A::mutableIterator, A::iterator)
    checkStrictSubtype(A::mutableListIterator, A::iterator)
    checkStrictSubtype(A::mutableListIterator, A::mutableIterator)
    checkStrictSubtype(A::mutableListIterator, A::listIterator)
    checkStrictSubtype(A::mutableMap, A::map)
    checkStrictSubtype(A::mutableMapEntry, A::mapEntry)
    checkStrictSubtype(A::mutableSet, A::iterable)
    checkStrictSubtype(A::mutableSet, A::mutableIterable)
    checkStrictSubtype(A::mutableSet, A::collection)
    checkStrictSubtype(A::mutableSet, A::mutableCollection)
    checkStrictSubtype(A::mutableSet, A::set)

    checkNotSubtype(A::list, A::mutableIterable)
    checkNotSubtype(A::list, A::mutableCollection)
    checkNotSubtype(A::mutableList, A::mutableSet)
    checkNotSubtype(A::map, A::mutableIterable)
    checkNotSubtype(A::map, A::mutableCollection)
    checkNotSubtype(A::mapEntry, A::mutableListIterator)

    return "OK"
}
