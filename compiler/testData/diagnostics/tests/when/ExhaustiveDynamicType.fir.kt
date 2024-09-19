// DIAGNOSTICS: -UNSUPPORTED
// ISSUE: KT-71601

fun subject(): dynamic = null

fun testNoCases() {
    val result = when (subject()) {
    }
}

fun testElse() {
    val result = when (subject()) {
        else -> ""
    }
}

fun testAny() {
    val result = when (subject()) {
        is Any -> ""
    }
}

fun testNullableAny() {
    val result = when (subject()) {
        <!USELESS_IS_CHECK!>is Any?<!> -> ""
    }
}

fun testAnyAndNull() {
    val result = when (subject()) {
        is Any -> ""
        null -> ""
    }
}
