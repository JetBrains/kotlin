// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +BreakContinueInInlineLambdas
// ISSUE: KT-1436

fun test() {
    for (i in 1..10) {
        lambdaAny {
            if (i == 3) <!UNSUPPORTED_FEATURE!>continue<!>
            if (i == 5) <!UNSUPPORTED_FEATURE!>break<!>
        }
    }
}

<!NOTHING_TO_INLINE!>inline<!> fun lambdaAny(p: Any) {
    p.toString()
}
