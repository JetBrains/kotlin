class A {
    fun foo() = 1

    fun B.bar() = 3
}

class B {
    fun foo() = 2

    fun A.bar() = 4
}

fun test(a: A, b: B) {
    with(b) {
        with(a) {
            foo()
            bar()
        }
    }
    with(a) {
        with(b) {
            foo()
            bar()
        }
    }
}
