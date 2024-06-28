// FIR_IDENTICAL
// LANGUAGE: -BreakContinueInInlineLambdas
// RENDER_DIAGNOSTICS_FULL_TEXT
// WITH_STDLIB

inline fun foo(block: () -> Unit) { block() }

inline fun bar(block1: () -> Unit, noinline block2: () -> Unit, crossinline block3: () -> Unit = {}) {
    block1()
    block2()
    block3()
}

fun test1() {
    while (true) {
        foo { <!UNSUPPORTED_FEATURE!>break<!> }
        foo { <!UNSUPPORTED_FEATURE!>continue<!> }
        foo(fun () { <!UNSUPPORTED_FEATURE!>break<!> })
        foo(fun () { <!UNSUPPORTED_FEATURE!>continue<!> })
    }
}

fun test2() {
    while (true) {
        bar({<!UNSUPPORTED_FEATURE!>break<!>}, {})
        bar({<!UNSUPPORTED_FEATURE!>continue<!>}, {})

        bar(fun () {<!UNSUPPORTED_FEATURE!>break<!>}, fun () {})
        bar(fun () {<!UNSUPPORTED_FEATURE!>continue<!>}, fun () {})
    }
}

fun test3() {
    while (true) {
        bar({}, { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }, { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
        bar({}, { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> }, { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
    }
}
