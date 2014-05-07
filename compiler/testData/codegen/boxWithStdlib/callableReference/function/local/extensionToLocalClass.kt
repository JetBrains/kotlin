fun box(): String {
    class A
    fun A.foo() = "OK"
    return (::A)().(A::foo)()
}
