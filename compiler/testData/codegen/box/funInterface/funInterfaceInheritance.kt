// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SAM_CONVERSIONS
// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

fun interface Base {
    fun doStuff(): String
}

fun interface I : Base

fun interface Proxy : I {

    override fun doStuff(): String = doStuffInt().toString()

    fun doStuffInt(): Int
}

fun runBase(b: Base) = b.doStuff()

fun runI(i: I) = i.doStuff()

fun runProxy(p: Proxy) = p.doStuff()

fun box(): String {

    if (runI { "i" } != "i") return "fail1"

    if (runProxy { 10 } != "10") return "fail2"

    return runBase { "OK" }
}