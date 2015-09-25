// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    operator fun get(i: Int): C = this
}

operator fun C.plus(a: Any): C = this
operator fun C.plusAssign(a: Any) {}

class C1 {
    operator fun get(i: Int): C = C()
    operator fun set(i: Int, v: C) {}
}

fun test() {
    val c = C()
    c[0] += ""
    var c1 = C1()
    c1[0] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> ""
}