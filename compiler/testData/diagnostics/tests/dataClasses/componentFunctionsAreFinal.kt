open data class A(val x: Int, val y: String)

class B : A(42, "OK") {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun component1(): Int = 21
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun component2(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Int<!> = 21
}

fun foo(b: B) {
    b.component1()
    b.component2()
}
