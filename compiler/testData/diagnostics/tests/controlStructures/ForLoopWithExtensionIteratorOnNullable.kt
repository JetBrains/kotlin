// See also KT-7428
class Container<K>(val k: K)
// iterator() must be an extension, otherwise code will not compile
operator fun <K> Container<K>.iterator(): Iterator<K> = null!!

fun test() {
    val container: Container<String>? = null
    // Error
    container<!UNSAFE_CALL!>.<!>iterator()
    // for extension iterator, this code compiles, but should not
    for (s in <!ITERATOR_MISSING!>container<!>) {}
}
class OtherContainer<K>(val k: K) {
    operator fun iterator(): Iterator<K> = null!!
}

fun test2() {
    val other: OtherContainer<String>? = null
    // Error
    for (s in <!ITERATOR_MISSING!>other<!>) {}
}
