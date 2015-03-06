// !DIAGNOSTICS: -UNUSED_PARAMETER

class C

fun C.plus(a: Any): C = this
fun C.plusAssign(a: Any) {}

fun test() {
    val c = C()
    c += ""
    var c1 = C()
    c1 <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> ""
}