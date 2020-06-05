// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument
// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_DCE_DRIVEN

fun interface MyRunnable {
    fun run()
}

fun box(): String {
    var result = "failed"
    val r = MyRunnable { result += "K" }
    foo({ result = "O" }, r)
    return result
}

fun foo(vararg rs: MyRunnable) {
    for (r in rs) {
        r.run()
    }
}
