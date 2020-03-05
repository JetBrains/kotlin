// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_DCE_DRIVEN

// MODULE: m1
// FILE: m1.kt

fun interface I {
    fun f(): String
}

fun rn(i: I) = i.f()

inline fun rnInline(i: I) = i.f()

// MODULE: m2(m1)
// FILE: m2.kt

fun rn2(f: () -> String) = rn(f)

inline fun rn2Inline(noinline f: () -> String) = rnInline(f)

// MODULE: main(m2)
// FILE: main.kt

fun box(): String {

    if (rn2Inline { "inline" } != "inline") return "fail"

    return rn2 { "OK" }
}