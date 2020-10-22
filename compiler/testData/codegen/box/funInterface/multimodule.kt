// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SAM_CONVERSIONS
// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

// MODULE: m1
// FILE: m1.kt

fun interface I {
    fun f(): String
}

fun rn(i: I) = i.f()

inline fun rnInline(i: I) = i.f()

inline fun rnInlineCtor(s: String) = I { s + ".m1" }

// MODULE: m2(m1)
// FILE: m2.kt

fun rn2(f: () -> String) = rn(f)

inline fun rn2Inline(noinline f: () -> String) = rnInline(f)

inline fun rnInlineCtorProxy(s: String) = rnInlineCtor(s + ".m2").f()

fun interface II {
    fun f(): String
}

// MODULE: main(m2)
// FILE: main.kt

fun id(i: II) = i

fun box(): String {

    if (id { "1" }.f() != "1") return "fail 1"

    if (II { "2" }.f() != "2") return "fail 2"

    if (rnInlineCtorProxy("3") != "3.m2.m1") return "fail 3"

    if (rn2Inline { "inline" } != "inline") return "fail 4"

    return rn2 { "OK" }
}