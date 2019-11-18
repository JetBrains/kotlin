// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    fun foo(s: String) = s
    return (::foo)("OK")
}
