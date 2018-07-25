// IGNORE_BACKEND: JVM_IR
fun box(): String {
    fun foo() = "OK"
    return (::foo)()
}
