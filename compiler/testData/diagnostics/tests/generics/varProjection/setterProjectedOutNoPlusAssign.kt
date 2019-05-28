// !DIAGNOSTICS: -UNREACHABLE_CODE
// !WITH_NEW_INFERENCE
interface Tr<T> {
    var v: T
}

fun test(t: Tr<out String>) {
    // resolved as t.v = t.v + null!!, where type of right operand is String,
    // so TYPE_MISMATCH: String is not <: of Captured(out String)
    <!SETTER_PROJECTED_OUT!>t.v<!> += null!!
}