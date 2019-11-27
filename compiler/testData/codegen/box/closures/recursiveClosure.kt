// IGNORE_BACKEND_FIR: JVM_IR
fun foo(s: String): String {
    fun bar(count: Int): String =
        if (count == 0) s else bar(count - 1)
    return bar(10)
}

fun box(): String = foo("OK")
