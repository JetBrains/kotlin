// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    fun <T> foo(t: T) = t

    return foo("OK")
}