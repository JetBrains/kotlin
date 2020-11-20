// !LANGUAGE: +NewInference +SamConversionForKotlinFunctions +SamConversionPerArgument +FunctionalInterfaceConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -NOTHING_TO_INLINE

fun interface SuspendRunnable {
    <!FUN_INTERFACE_WITH_SUSPEND_FUNCTION!>suspend<!> fun invoke()
}

fun run(r: SuspendRunnable) {}

suspend fun bar() {}

fun test() {
    run(::bar)
}
