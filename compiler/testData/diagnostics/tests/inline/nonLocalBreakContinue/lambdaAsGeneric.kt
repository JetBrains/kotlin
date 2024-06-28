// LANGUAGE: +BreakContinueInInlineLambdas
// ISSUE: KT-1436

fun test() {
    for (i in 1..10) {
        lambda {
            if (i == 3) <!UNSUPPORTED_FEATURE!>continue<!>
            if (i == 5) <!UNSUPPORTED_FEATURE!>break<!>
        }
    }
}

<!NOTHING_TO_INLINE!>inline<!> fun <T> lambda(p: T) { }
