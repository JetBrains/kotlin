// IGNORE_BACKEND_FIR: JVM_IR
class A() {
    companion object {
        val value = 10
    }
}

fun box() = if (A.value == 10) "OK" else "Fail ${A.value}"
