// !DIAGNOSTICS: -UNREACHABLE_CODE
interface Tr<T> {
    var v: T
}

fun test(t: Tr<out String>) {
    t.<!SETTER_PROJECTED_OUT!>v<!> += null!!
}