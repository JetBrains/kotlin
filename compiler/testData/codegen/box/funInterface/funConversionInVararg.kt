// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument

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
