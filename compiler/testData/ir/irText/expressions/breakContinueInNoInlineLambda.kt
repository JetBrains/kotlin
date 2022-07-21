// LANGUAGE: +BreakContinueInInlineLambdas
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// !IGNORE_ERRORS


inline fun foo(noinline block: () -> Unit) = block()


fun test1() {
    L1@ while (true) {
        foo { break }

        foo { break@L1 }

        foo { continue }

        foo { continue@L1 }
    }
}