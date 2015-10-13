class A(val foo: Int)

fun A.test(foo: String) {
    val <!UNUSED_VARIABLE!>a<!>: String = foo
}