// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SAM_CONVERSIONS
// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

fun interface Foo {
    fun invoke(): String
}

fun foo(f: Foo) = f.invoke()

fun box(): String {
    return foo { "OK" }
}