fun <K, V> testMutableMapEntry(<warning>map</warning>: MutableMap<K, V>, <warning>k1</warning>: K, <warning>v</warning>: V) {
}

fun foo() {
    <error>testMutableMapEntry</error><error>(hashMap(1 to 'a'), 'b')</error>
}

//extract from library
fun <K, V> hashMap(<warning>p</warning>: Pair<K, V>): MutableMap<K, V> {<error>}</error>
fun <K, V> K.to(<warning>v</warning>: V): Pair<K, V> {<error>}</error>
data class Pair<K, V> {}