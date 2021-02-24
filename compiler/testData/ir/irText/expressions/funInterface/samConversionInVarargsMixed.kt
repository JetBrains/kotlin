// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument
fun interface MyRunnable {
    fun run()
}

fun test(a: Any, r: MyRunnable) {
    if (a is MyRunnable) {
        foo({}, r, a)
    }
}

fun foo(vararg rs: MyRunnable) {}
