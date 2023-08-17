// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// CHECK_BYTECODE_LISTING
// ^ Check that there's only one $EntriesIntrinsicMappings class, with three fields (entries$0, entries$1, entries$2).

// MODULE: lib
// !LANGUAGE: -EnumEntries
// FILE: X.kt

enum class X {
    X1, X2
}

// FILE: Y.java

public enum Y {
    Y1, Y2
}

// FILE: Z.java

public enum Z {
    Z1, Z2
}

// MODULE: box(lib)
// !LANGUAGE: +EnumEntries
// !OPT_IN: kotlin.ExperimentalStdlibApi
// FILE: box.kt

import kotlin.enums.enumEntries

fun box(): String {
    val x = enumEntries<X>()
    if (x.toString() != "[X1, X2]") return "Fail X: $x"

    val y = enumEntries<Y>()
    if (y.toString() != "[Y1, Y2]") return "Fail Y: $y"

    val z = enumEntries<Z>()
    if (z.toString() != "[Z1, Z2]") return "Fail Z: $z"

    val xx = enumEntries<X>()
    if (xx.toString() != "[X1, X2]") return "Fail X #2: $xx"

    return "OK"
}
