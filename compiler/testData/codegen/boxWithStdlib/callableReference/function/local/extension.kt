class A

fun box(): String {
    fun A.foo() = "OK"
    return A().(A::foo)()
}
