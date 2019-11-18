// !LANGUAGE: +NewInference +FunctionInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

// FILE: A.kt

fun interface KRunnable {
    fun invoke(): String
}

fun inA(k: KRunnable): String = k.invoke()

// FILE: B.kt

fun inB(k: KRunnable): String = k.invoke()

fun box(): String {
    val first = inA(KRunnable { "O" }) + inB(KRunnable { "K" })
    if (first != "OK") return "fail1: $first"

    val second = inA { "O" } + inB { "K" }
    if (second != "OK") return "fail2: $second"

    val f1: () -> String = { "O" }
    val f2: () -> String = { "K" }

    val third = inA(f1) + inB(f2)
    if (third != "OK") return "fail3: $third"

    return "OK"
}
