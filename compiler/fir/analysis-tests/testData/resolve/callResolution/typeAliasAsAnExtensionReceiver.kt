// FIR_IDENTICAL
object Type1
object Type2

typealias ImmutableMap<K, V> = java.util.Map<K, V>
private typealias ImmutableMultimap<K, V> = ImmutableMap<K, Set<V>>

fun test(t2: ImmutableMap<Type1, Set<Type2>>) {
    var resultingTypeInfo = t2
    resultingTypeInfo.foo(Type1, Type2)
}

private fun <K, V> ImmutableMultimap<K, V>.foo(key: K, value: V) {
}