// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    fun get(i: Int): C = this
}

fun C.plus(a: Any): C = this
fun C.plusAssign(a: Any) {}

class C1 {
    fun get(i: Int): C = C()
    fun set(i: Int, v: C) {}
}

fun test() {
    val c = C()
    c[0] += ""
    var c1 = C1()
    c1[0] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> ""
}