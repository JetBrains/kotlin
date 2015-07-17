// !DIAGNOSTICS: -UNUSED_PARAMETER

import java.util.*

interface Foo
class Bar<B : Foo>(val list: MutableList<B>) {}

fun <F : Foo> test(map: MutableMap<String, Bar<F>>) {

    map.getOrPut1("", { Bar(ArrayList()) })
}

fun <K, V> MutableMap<K, V>.getOrPut1(key: K, defaultValue: () -> V): V = throw Exception()
