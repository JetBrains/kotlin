fun foo() {
    val a = A()
    a()<caret>
}

class A {
    fun invoke() {}
}

// EXISTS: invoke()