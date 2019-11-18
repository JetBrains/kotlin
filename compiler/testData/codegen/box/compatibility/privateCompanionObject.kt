// IGNORE_BACKEND_FIR: JVM_IR
class Test {
    private companion object {
        val res = "OK"
    }
    fun res() = res
}

fun box(): String {
    return Test().res()
}
