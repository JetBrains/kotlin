fun box(): String {
    interface A {
        fun foo() = "OK"
    }

    class B : A

    return B().foo()
}