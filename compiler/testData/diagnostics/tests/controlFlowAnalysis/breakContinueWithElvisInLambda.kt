// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_STDLIB
// ISSUE: KT-67624

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun contractRun(f: (s: String?) -> Unit) {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    f(null)
}

inline fun inlineRun(f: (s: String?) -> Unit) {
    f(null)
}

fun noInlineRun(f: (s: String?) -> Unit) {
    f(null)
}

// ------------- continue -------------

fun test_1_continue() {
    while (true) {
        contractRun {
            val result = it ?: <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
        }
    }
}

fun test_2_continue() {
    while (true) {
        inlineRun {
            val result = it ?: <!UNSUPPORTED_FEATURE!>continue<!>
        }
    }
}

fun test_3_continue() {
    while (true) {
        noInlineRun {
            val result = it ?: <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
        }
    }
}

// ------------- break -------------

fun test_1_break() {
    while (true) {
        contractRun {
            val result = it ?: <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
        }
    }
}

fun test_2_break() {
    while (true) {
        inlineRun {
            val result = it ?: <!UNSUPPORTED_FEATURE!>break<!>
        }
    }
}

fun test_3_break() {
    while (true) {
        noInlineRun {
            val result = it ?: <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
        }
    }
}


