// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +RangeUntilOperator

fun foo() {
    val a: dynamic = Any()
    val b: dynamic = Any()
    val c = C()
    println(<!WRONG_OPERATION_WITH_DYNAMIC!>a..<b<!>)
    println(c..<a)
    println(a.rangeUntil(b))
}

class C {
    operator fun rangeUntil(other: dynamic): ClosedRange<dynamic> = TODO("not implemented")
}
