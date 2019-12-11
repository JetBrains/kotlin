open class A : B()
open class B : <!OTHER_ERROR!>A<!>()

fun <T> select(vararg xs: T): T = xs[0]

fun foo() {
    val x = select(A(), B(), "foo")
}