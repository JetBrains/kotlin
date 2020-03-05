// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

// IGNORE_BACKEND: JVM, JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_DCE_DRIVEN

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