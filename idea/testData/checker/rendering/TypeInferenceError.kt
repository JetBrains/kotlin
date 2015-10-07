fun <K, V> testMutableMapEntry(<warning>map</warning>: MutableMap<K, V>, <warning>k1</warning>: K, <warning>v</warning>: V) {
}

fun foo() {
    <error>testMutableMapEntry</error>(hashMap(1 to 'a'), 'b'<error>)</error>
}

//extract from library
fun <K, V> hashMap(<warning>p</warning>: Pair<K, V>): MutableMap<K, V> {<error>}</error>
infix fun <K, V> K.to(<warning>v</warning>: V): Pair<K, V> {<error>}</error>
class Pair<K, V> {}