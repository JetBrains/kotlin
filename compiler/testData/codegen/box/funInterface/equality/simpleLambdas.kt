// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND_FIR: JVM_IR

fun interface FunInterface {
    fun invoke()
}

private fun id(f: FunInterface): Any = f

fun box(): String {
    if (id { "lambda" } == id { "lambda" }) return "Fail: SAMs over lambdas are never equal"

    return "OK"
}
