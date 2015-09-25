// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    val c: C = C()
}

operator fun C.plus(a: Any): C = this
operator fun C.plusAssign(a: Any) {}

class C1 {
    var c: C = C()
}

fun test() {
    val c = C()
    c.c += ""
    var c1 = C1()
    c1.c <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> ""
}