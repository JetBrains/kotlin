// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

fun <K, V> testMutableMapEntry(map: MutableMap<K, V>, k1: K, v: V) {
}

fun foo() {
    testMutableMapEntry(hashMap(1 to 'a'), 'b'<error descr="[NO_VALUE_FOR_PARAMETER] No value passed for parameter 'v'">)</error>
}

//extract from library
fun <K, V> hashMap(p: Pair<K, V>): MutableMap<K, V> {}
infix fun <K, V> K.to(v: V): Pair<K, V> {}
class Pair<K, V> {}
