// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-68613

interface Generic<out T>

typealias TA<K> = (String) -> Generic<K>

fun test(it: Any) {
    val that = it <!UNCHECKED_CAST!>as Generic<TA<out Any>><!>
}

fun rest(it: Any) {
    val that = it <!UNCHECKED_CAST!>as Generic<<!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<in Any><!>><!>
}
