// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +SamConversionForKotlinFunctions +SamConversionPerArgument +FunctionalInterfaceConversion
// DIAGNOSTICS: -UNUSED_PARAMETER

fun interface KRunnable {
    fun invoke()
}

typealias KRunnableAlias = KRunnable

fun foo(f: KRunnable) {}

fun test() {
    foo(KRunnable {})
    foo(KRunnableAlias {})
}
