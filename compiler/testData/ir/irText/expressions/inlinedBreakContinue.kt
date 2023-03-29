// FIR_IDENTICAL
// LANGUAGE: +BreakContinueInInlineLambdas
// DUMP_LOCAL_DECLARATION_SIGNATURES
// TARGET_BACKEND: JVM_IR
// IGNORE_ERRORS
// WITH_STDLIB

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57428

inline fun foo(block: () -> Unit) { block() }

inline fun bar(block1: () -> Unit, noinline block2: () -> Unit) {
    block1()
    block2()
}

fun test1() {
    while (true) {
        foo { break }
        foo { continue }
        foo(fun () { break })
        foo(fun () { continue })
    }
}

fun test2() {
    while (true) {
        bar({break}, {})
        bar({continue}, {})

        bar(fun () {break}, fun () {})
        bar(fun () {continue}, fun () {})
    }
}
