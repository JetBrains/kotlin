fun box(): String {
    trait A {
        fun foo() = "OK"
    }

    class B : A

    return B().foo()
}