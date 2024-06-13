// FIR_IDENTICAL
// LANGUAGE: +BreakContinueInInlineLambdas
// ISSUE: KT-1436

inline fun Any.myRunInlineExtension(block: () -> Unit) = block()

fun test() {
    for (i in 0..10) {
        "".myRunInlineExtension {
            if (i == 2) continue
            if (i == 3) break
        }
    }
}