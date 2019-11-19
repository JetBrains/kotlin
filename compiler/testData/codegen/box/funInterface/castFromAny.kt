// !LANGUAGE: +NewInference +FunctionInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

fun interface KRunnable {
    fun invoke()
}

fun test(a: Any?) {
    a as () -> Unit
    KRunnable(a).invoke()
}

fun box(): String {
    var result = "Fail"
    test {
        result = "OK"
    }
    return result
}
