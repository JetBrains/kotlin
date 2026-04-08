// FIR_IDENTICAL
// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_ERRORS
// WITH_STDLIB

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
