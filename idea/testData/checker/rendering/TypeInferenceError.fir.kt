// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

fun <K, V> testMutableMapEntry(map: MutableMap<K, V>, k1: K, v: V) {
}

fun foo() {
    <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /testMutableMapEntry">testMutableMapEntry</error>(hashMap(1 to 'a'), 'b')
}

//extract from library
fun <K, V> hashMap(p: Pair<K, V>): MutableMap<K, V> {}
infix fun <K, V> K.to(v: V): Pair<K, V> {}
class Pair<K, V> {}
