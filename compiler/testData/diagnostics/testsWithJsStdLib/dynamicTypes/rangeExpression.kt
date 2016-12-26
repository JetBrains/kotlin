// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    val a: dynamic = Any()
    val b: dynamic = Any()
    val c = C()
    println(a<!WRONG_OPERATION_WITH_DYNAMIC!>..<!>b)
    println(c..a)
    println(a.rangeTo(b))
}

class C {
    operator fun rangeTo(other: dynamic): ClosedRange<dynamic> = TODO("not implemented")
}