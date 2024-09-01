// LANGUAGE: +BreakContinueInInlineLambdas
// ISSUE: KT-1436

inline fun Any.myRunInlineExtension(block: () -> Unit) = block()

fun test() {
    for (i in 0..10) {
        "".myRunInlineExtension {
            if (i == 2) <!UNSUPPORTED_FEATURE!>continue<!>
            if (i == 3) <!UNSUPPORTED_FEATURE!>break<!>
        }
    }
}
