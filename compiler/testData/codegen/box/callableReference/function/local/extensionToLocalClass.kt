fun box(): String {
    class A
    fun A.foo() = "OK"
    return (A::foo)((::A)())
}
