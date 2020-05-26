// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions
// WITH_RUNTIME
// SKIP_DCE_DRIVEN

fun interface MyRunnable {
    fun invoke()
}

class A {
    inline fun doWork(noinline job: () -> Unit) {
        MyRunnable(job).invoke()
    }

    fun doNoninlineWork(job: () -> Unit) {
        MyRunnable(job).invoke()
    }
}

fun box(): String {
    var result = false
    A().doWork { result = true }
    if (!result) return "Fail 1"

    result = false
    A().doNoninlineWork { result = true }
    if (!result) return "Fail 2"

    return "OK"
}
