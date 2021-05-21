// !API_VERSION: LATEST
// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JVM
// WITH_RUNTIME

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
    // TODO: java.util.Collection<java.lang.Character>
    check("java.util.Collection<char>", typeOf<Collection<Char>>())
    check("java.util.List<java.lang.String>", typeOf<List<String>>())
    check("java.util.Set<java.lang.Double?>", typeOf<Set<Double?>>())
    check("java.util.ListIterator<java.lang.Object>", typeOf<ListIterator<Any>>())
    // TODO: java.util.Map<java.lang.Integer, kotlin.Unit>
    check("java.util.Map<int, kotlin.Unit>", typeOf<Map<Int, Unit>>())
    check("java.util.Map\$Entry<java.lang.Byte?, java.util.Set<*>>", typeOf<Map.Entry<Byte?, Set<*>>>())

    check("java.lang.Iterable<java.lang.Number>", typeOf<MutableIterable<Number>>())
    check("java.util.Iterator<java.lang.CharSequence>", typeOf<MutableIterator<CharSequence>>())
    check("java.util.Collection<char>", typeOf<MutableCollection<Char>>())
    check("java.util.List<java.lang.String>", typeOf<MutableList<String>>())
    check("java.util.Set<java.lang.Double?>", typeOf<MutableSet<Double?>>())
    check("java.util.ListIterator<java.lang.Object>", typeOf<MutableListIterator<Any>>())
    check("java.util.Map<int, kotlin.Unit>", typeOf<MutableMap<Int, Unit>>())
    check("java.util.Map\$Entry<java.lang.Byte?, java.util.Set<*>>", typeOf<MutableMap.MutableEntry<Byte?, Set<*>>>())

    return "OK"
}
