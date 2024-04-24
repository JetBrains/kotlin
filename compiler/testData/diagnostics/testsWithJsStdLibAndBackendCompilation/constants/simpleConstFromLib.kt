// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
// MODULE: lib1
// FILE: A.kt

const val constValA = "constValA"

// FILE: B.kt

const val constValB = constValA + "constValB"

// MODULE: main(lib1)
// FILE: Main.kt

fun box(): String {
    val x = "var $constValA = '$constValB';"
    if (x != "var constValA = 'constValAconstValB';") {
        return x
    }
    return "OK"
}
