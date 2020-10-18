// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SAM_CONVERSIONS
// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

// FILE: lib.kt

fun interface Foo {
    fun invoke(): String
}

// FILE: f1.kt

fun foo1(f: Foo) = f.invoke()

inline fun bar1(): String {
    val f: () -> String = { "O" }
    return foo1(Foo(f))
}

// FILE: f2.kt

fun foo2(f: Foo) = f.invoke()

inline fun bar2(): String {
    val f: () -> String = { "K" }
    return foo2(Foo(f))
}

// FILE: main.kt

fun box(): String = bar1() + bar2()