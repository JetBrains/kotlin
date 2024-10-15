// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// NI_EXPECTED_FILE

fun foo(first: Array<Any?>, second: Array<Any?>) = Pair(first.toCollection<!NO_VALUE_FOR_PARAMETER!>()<!>, second.toCollection<!NO_VALUE_FOR_PARAMETER!>()<!>)
