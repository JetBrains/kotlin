// LANGUAGE: +BreakContinueInInlineLambdas
// ISSUE: KT-1436

fun test() {
    for (i in 1..10) {
        lambda {
            if (i == 3) continue
            if (i == 5) break
        }
    }
}

<!NOTHING_TO_INLINE!>inline<!> fun <T> lambda(p: T) { }