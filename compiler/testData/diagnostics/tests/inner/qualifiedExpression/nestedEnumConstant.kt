class A {
    enum class E {
        E1,
        E2 { };
    }
}

fun foo() = A.E.E1
fun bar() = A.E.E2
