// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
// !CHECK_TYPE
// t is unused due to KT-4233
trait Tr<T> {
    var v: T
}

fun test(t: Tr<*>) {
    t.<!SETTER_PROJECTED_OUT!>v<!> = null!!
    t.v checkType { _<Any?>() }
}