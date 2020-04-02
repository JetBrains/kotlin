// FIR_IDENTICAL
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -NAME_SHADOWING -UNUSED_VARIABLE

fun foo(x: Int) {
    val x = if (true) { // OI: Map<String, () → Int>?, NI: Nothing?, error
        "" to { x }
    } else { null }
}
