// IGNORE_BACKEND: JVM_IR
fun box(): String {
    val result = "OK"

    fun foo() = result

    return (::foo)()
}
