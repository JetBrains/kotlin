// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A(val map: MutableMap<String, CharSequence>) {

    var a: String by map.withDefault1 { "foo" }
}

operator fun <G> MutableMap<in String, in G>.getValue(thisRef: Any?, property: KProperty<*>): G = throw Exception()

operator fun <S> MutableMap<in String, in S>.setValue(thisRef: Any?, property: KProperty<*>, value: S) {}

fun <K, V> MutableMap<K, V>.withDefault1(default: (key: K) -> V): MutableMap<K, V> = this
