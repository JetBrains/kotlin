// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    fun foo() = "OK"
    return (::foo)()
}
