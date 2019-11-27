// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    class A
    fun A.foo() = "OK"
    return (A::foo)((::A)())
}
