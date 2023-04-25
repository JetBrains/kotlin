// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    val a: dynamic = Any()
    val b: dynamic = Any()
    val c = C()
    println(<!WRONG_OPERATION_WITH_DYNAMIC!>a..b<!>)
    println(c..a)
    println(a.rangeTo(b))
}

class C {
    operator fun rangeTo(other: dynamic): ClosedRange<dynamic> = TODO("not implemented")
}
