class A {
    fun plus(a: A) {}
}

fun foo() {
    f1() + A() + A()<caret>
}

fun f1() = A()

// EXISTS: plus(A), f1()