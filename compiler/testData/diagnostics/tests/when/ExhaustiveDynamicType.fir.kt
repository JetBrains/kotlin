// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNSUPPORTED
// LANGUAGE: +ImprovedExhaustivenessChecksIn21
// ISSUE: KT-71601

fun subject(): dynamic = null

fun testNoCases() {
    val result = <!NO_ELSE_IN_WHEN!>when<!> (subject()) {
    }
}

fun testElse() {
    val result = when (subject()) {
        else -> ""
    }
}

fun testAny() {
    val result = <!NO_ELSE_IN_WHEN!>when<!> (subject()) {
        is Any -> ""
    }
}

fun testNullableAny() {
    val result = when (subject()) {
        is Any? -> ""
    }
}

fun testAnyAndNull() {
    val result = when (subject()) {
        is Any -> ""
        null -> ""
    }
}
