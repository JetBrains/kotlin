// IGNORE_BACKEND_FIR: JVM_IR
enum class Test {
    A, B, OTHER
}

fun peek() = Test.A

fun box(): String {
    val x = when (val type = peek()) {
        Test.A -> "OK"
        else -> "other"
    }
    return x
}
