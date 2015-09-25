class A {
    val b = B()
}
class B
operator fun B.invoke(i: Int) = i

fun foo(i: Int) = i

fun test(a: A?) {
    a?.b(1) //should be no warning
    foo(<!TYPE_MISMATCH!>a?.b(1)<!>) //no warning, only error
}
