// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR

private fun id(f: Runnable): Any = f

fun box(): String {
    if (id { "lambda" } == id { "lambda" }) return "Fail: SAMs over lambdas are never equal"

    return "OK"
}
