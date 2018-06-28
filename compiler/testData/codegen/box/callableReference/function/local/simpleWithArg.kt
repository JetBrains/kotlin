// IGNORE_BACKEND: JVM_IR
fun box(): String {
    fun foo(s: String) = s
    return (::foo)("OK")
}
