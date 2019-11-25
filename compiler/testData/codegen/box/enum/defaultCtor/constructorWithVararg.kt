// IGNORE_BACKEND_FIR: JVM_IR
enum class Test(vararg xs: Int) {
    OK;
    val values = xs
}

fun box(): String =
        if (Test.OK.values.size == 0) "OK" else "Fail"