// !DIAGNOSTICS: -UNUSED_PARAMETER

class C

operator fun C.plus(a: Any): C = this
operator fun C.plusAssign(a: Any) {}

fun test() {
    val c = C()
    c += ""
    var c1 = C()
    <!ASSIGN_OPERATOR_AMBIGUITY!>c1 += ""<!>
}