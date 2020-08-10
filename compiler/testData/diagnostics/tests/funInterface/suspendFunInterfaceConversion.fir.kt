// !LANGUAGE: +NewInference +SamConversionForKotlinFunctions +SamConversionPerArgument +FunctionalInterfaceConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -NOTHING_TO_INLINE

fun interface SuspendRunnable {
    suspend fun invoke()
}

fun run(r: SuspendRunnable) {}

suspend fun bar() {}

fun test() {
    run(::bar)
}
