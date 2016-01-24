class B

class A {
    operator fun B.invoke() {}
}

val B.a: () -> Int  get() = { 5 }

fun test(a: A, b: B) {
    val <!UNUSED_VARIABLE!>x<!>: Int = b.a()

    b.<!FUNCTION_EXPECTED!>(a)<!>()

    with(b) {
        val <!UNUSED_VARIABLE!>y<!>: Int = a()
        <!FUNCTION_EXPECTED!>(a)<!>()
    }
}
