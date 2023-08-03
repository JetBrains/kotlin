// TARGET_BACKEND: JVM_IR
// !OPT_IN: kotlin.ExperimentalStdlibApi
// WITH_STDLIB
// FILE: Z.java

public enum Z {
    O, K
}

// FILE: box.kt

import kotlin.enums.*

fun callFromOtherFunctionInTheSameFile(): EnumEntries<Z> = enumEntries<Z>()

fun box(): String {
    val z = enumEntries<Z>()
    if (z.toString() != "[O, K]") return "Fail 1: $z"

    val z2 = enumEntries<Z>()
    if (z2 !== z) return "Fail 2: another instance of EnumEntries is created"

    val z3 = callFromOtherFunctionInTheSameFile()
    if (z3 !== z) return "Fail 3: another instance of EnumEntries is created"

    return "OK"
}
