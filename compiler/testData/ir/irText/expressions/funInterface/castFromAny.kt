// FIR_IDENTICAL
// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

fun interface KRunnable {
    fun invoke()
}

fun test(a: Any?) {
    a as () -> Unit
    KRunnable(a).invoke()
}

