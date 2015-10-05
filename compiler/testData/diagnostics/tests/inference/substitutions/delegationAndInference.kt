// !DIAGNOSTICS: -UNUSED_PARAMETER

class A(val map: MutableMap<String, CharSequence>) {

    var a: String by map.withDefault1 { "foo" }
}

fun <G> MutableMap<in String, in G>.getValue(thisRef: Any?, property: PropertyMetadata): G = throw Exception()

fun <S> MutableMap<in String, in S>.setValue(thisRef: Any?, property: PropertyMetadata, value: S) {}

fun <K, V> MutableMap<K, V>.withDefault1(default: (key: K) -> V): MutableMap<K, V> = this