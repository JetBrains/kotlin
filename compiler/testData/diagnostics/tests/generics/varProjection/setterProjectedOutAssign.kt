// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
// !CHECK_TYPE
// !WITH_NEW_INFERENCE
// t is unused due to KT-4233
interface Tr<T> {
    var v: T
}

fun test(t: Tr<*>) {
    <!NI;SETTER_PROJECTED_OUT!>t.v<!> = null!!
    <!SETTER_PROJECTED_OUT!>t.v<!> = <!NI;TYPE_MISMATCH!>""<!>
    <!SETTER_PROJECTED_OUT!>t.v<!> = <!NI;NULL_FOR_NONNULL_TYPE!>null<!>

    t.v checkType { _<Any?>() }
}