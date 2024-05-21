// ISSUE: KT-68613

interface Generic<out T>

typealias TA<K> = (String) -> Generic<K>

fun test(it: Any) {
    val that = it <!UNCHECKED_CAST!>as Generic<TA<<!CONFLICTING_PROJECTION!>out<!> Any>><!>
}

fun rest(it: Any) {
    val that = it <!UNCHECKED_CAST!>as Generic<TA<<!REDUNDANT_PROJECTION!>in<!> Any>><!>
}
