open class A {
    fun foo() = 42

    object B: A()
}

fun test() {
    A.B.(A::foo)()
}