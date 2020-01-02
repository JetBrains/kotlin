// KT-2825  DataFlowInfo is not retained after assignment

interface A

interface B : A {
    fun foo()
}

fun baz(b: B) = b

fun bar1(a: A) {
    val b = a as B
    a.foo()
    b.foo()
}

fun bar2(a: A) {
    val b = baz(a as B)
    a.foo()
    b.foo()
}
