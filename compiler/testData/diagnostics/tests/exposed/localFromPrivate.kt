class A {
    private open class B
    fun f() {
        // Local from private: Ok
        class C : B()
    }
}

private open class D

fun f(): Int {
    // Local from private: Ok
    val x = object : D() { }
    return x.hashCode()
}