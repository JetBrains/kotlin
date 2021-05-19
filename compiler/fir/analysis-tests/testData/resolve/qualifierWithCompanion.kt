package my

class A {
    companion object X {
        fun foo() {}
    }
}

val xx = A()

fun test() {
    val x = A
    A.foo()
    A.X.foo()

    fun A.invoke() {}

    my.<!OPERATOR_MODIFIER_REQUIRED!>xx<!>()
}
