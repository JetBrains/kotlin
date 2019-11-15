// !LANGUAGE: +NewInference +SamConversionForKotlinFunctions +SamConversionPerArgument +FunctionInterfaceConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun interface KRunnable {
    fun invoke()
}

typealias KRunnableAlias = KRunnable

fun foo(f: KRunnable) {}

fun test() {
    foo(KRunnable {})
    foo(KRunnableAlias {})
}