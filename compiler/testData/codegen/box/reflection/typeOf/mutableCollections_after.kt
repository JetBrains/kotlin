// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.test.assertEquals

fun check(expected: String, actual: KType) {
    assertEquals(expected, actual.toString())
}

fun box(): String {
    check("kotlin.collections.Iterable<kotlin.Number>", typeOf<Iterable<Number>>())
    check("kotlin.collections.Iterator<kotlin.CharSequence>", typeOf<Iterator<CharSequence>>())
    check("kotlin.collections.Collection<kotlin.Char>", typeOf<Collection<Char>>())
    check("kotlin.collections.List<kotlin.String>", typeOf<List<String>>())
    check("kotlin.collections.Set<kotlin.Double?>", typeOf<Set<Double?>>())
    check("kotlin.collections.ListIterator<kotlin.Any>", typeOf<ListIterator<Any>>())
    check("kotlin.collections.Map<kotlin.Int, kotlin.Unit>", typeOf<Map<Int, Unit>>())
    check("kotlin.collections.Map.Entry<kotlin.Byte?, kotlin.collections.Set<*>>", typeOf<Map.Entry<Byte?, Set<*>>>())

    check("kotlin.collections.MutableIterable<kotlin.Number>", typeOf<MutableIterable<Number>>())
    check("kotlin.collections.MutableIterator<kotlin.CharSequence>", typeOf<MutableIterator<CharSequence>>())
    check("kotlin.collections.MutableCollection<kotlin.Char>", typeOf<MutableCollection<Char>>())
    check("kotlin.collections.MutableList<kotlin.String>", typeOf<MutableList<String>>())
    check("kotlin.collections.MutableSet<kotlin.Double?>", typeOf<MutableSet<Double?>>())
    check("kotlin.collections.MutableListIterator<kotlin.Any>", typeOf<MutableListIterator<Any>>())
    check("kotlin.collections.MutableMap<kotlin.Int, kotlin.Unit>", typeOf<MutableMap<Int, Unit>>())
    check("kotlin.collections.MutableMap.MutableEntry<kotlin.Byte?, kotlin.collections.Set<*>>", typeOf<MutableMap.MutableEntry<Byte?, Set<*>>>())

    check(typeOf<MutableList<Any>>().isSubtypeOf(typeOf<List<Any>>()))
    check(!typeOf<List<Any>>().isSubtypeOf(typeOf<MutableList<Any>>()))
    check(typeOf<List<Any>>().isSupertypeOf(typeOf<MutableList<Any>>()))
    check(!typeOf<MutableList<Any>>().isSupertypeOf(typeOf<List<Any>>()))

    return "OK"
}
