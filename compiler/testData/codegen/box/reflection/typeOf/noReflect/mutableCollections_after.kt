// TARGET_BACKEND: JVM
// WITH_STDLIB

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

fun check(expected: String, actual: KType) {
    assertEquals(expected + " (Kotlin reflection is not available)", actual.toString())
}

fun box(): String {
    check("java.lang.Iterable<java.lang.Number>", typeOf<Iterable<Number>>())
    check("java.util.Iterator<java.lang.CharSequence>", typeOf<Iterator<CharSequence>>())
    check("java.util.Collection<java.lang.Character>", typeOf<Collection<Char>>())
    check("java.util.List<java.lang.String>", typeOf<List<String>>())
    check("java.util.Set<java.lang.Double?>", typeOf<Set<Double?>>())
    check("java.util.ListIterator<java.lang.Object>", typeOf<ListIterator<Any>>())
    check("java.util.Map<java.lang.Integer, kotlin.Unit>", typeOf<Map<Int, Unit>>())
    check("java.util.Map\$Entry<java.lang.Byte?, java.util.Set<*>>", typeOf<Map.Entry<Byte?, Set<*>>>())

    check("java.lang.Iterable<java.lang.Number>", typeOf<MutableIterable<Number>>())
    check("java.util.Iterator<java.lang.CharSequence>", typeOf<MutableIterator<CharSequence>>())
    check("java.util.Collection<java.lang.Character>", typeOf<MutableCollection<Char>>())
    check("java.util.List<java.lang.String>", typeOf<MutableList<String>>())
    check("java.util.Set<java.lang.Double?>", typeOf<MutableSet<Double?>>())
    check("java.util.ListIterator<java.lang.Object>", typeOf<MutableListIterator<Any>>())
    check("java.util.Map<java.lang.Integer, kotlin.Unit>", typeOf<MutableMap<Int, Unit>>())
    check("java.util.Map\$Entry<java.lang.Byte?, java.util.Set<*>>", typeOf<MutableMap.MutableEntry<Byte?, Set<*>>>())

    return "OK"
}
