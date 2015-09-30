// !DIAGNOSTICS: -UNUSED_PARAMETER

class GenericClass<out T>

public fun <K, V> GenericClass<Map<K, V>>.foo() {}

public fun <T> bar(t: T, ext: GenericClass<T>.() -> Unit) {}

fun test() {
    bar(mapOf(2 to 3)) { foo() }
}

// from library
class Pair<out A, out B>
fun <K, V> mapOf(keyValuePair: Pair<K, V>): Map<K, V> = throw Exception()
infix fun <A, B> A.to(that: B): Pair<A, B> = throw Exception()

