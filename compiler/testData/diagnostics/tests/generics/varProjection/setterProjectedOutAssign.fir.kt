// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
// !CHECK_TYPE
// !WITH_NEW_INFERENCE
// t is unused due to KT-4233
interface Tr<T> {
    var v: T
}

fun test(t: Tr<*>) {
    t.v = null!!
    t.v = <!ASSIGNMENT_TYPE_MISMATCH!>""<!>
    t.v = <!NULL_FOR_NONNULL_TYPE!>null<!>
    t.v checkType { _<Any?>() }
}
