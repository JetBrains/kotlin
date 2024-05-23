// ISSUE: KT-67764

interface Generic<out T>

typealias TA<K> = (String) -> Generic<K>

fun test(it: Any) {
    val that = it <!UNCHECKED_CAST!>as TA<out Any><!>
}

fun rest(it: Any) {
    val that = it <!UNCHECKED_CAST!>as TA<in Any><!>
}
