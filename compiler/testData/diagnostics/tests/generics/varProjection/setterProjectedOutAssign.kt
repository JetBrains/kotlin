// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
// !CHECK_TYPE
// t is unused due to KT-4233
trait Tr<T> {
    var v: T
}

fun test(t: Tr<*>) {
    <!SETTER_PROJECTED_OUT!>t.v<!> = null!!
    t.v checkType { it : _<Any?> }
}