// DIAGNOSTICS: -NAME_SHADOWING -UNUSED_VARIABLE -UNUSED_EXPRESSION

fun foo1(x: Int) {
    val x = if (true) { // OI: Map<String, () → Int>?, NI: Nothing?, error
        "" to { x }
    } else { null }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Pair<kotlin.String, () -> kotlin.Int>?")!>x<!>
}

fun foo2(x: Int) {
    val x = if (true) {
        mapOf("" to { x }) // `Map<String, () → Int>` is in type info in IDE
    } else {
        null
    } // Fixed the problem: "type of entire `if` is `Map<String, *>?` (NI) instead of `Map<String, () → Int>?` (OI)"

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<kotlin.String, () -> kotlin.Int>?")!>x<!>
}
