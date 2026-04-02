// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +BreakContinueInInlineLambdas
// ISSUE: KT-1436

fun test() {
    for (i in 1..10) {
        lambdaAny {
            if (i == 3) <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            if (i == 5) <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
        }
    }
}

<!NOTHING_TO_INLINE!>inline<!> fun lambdaAny(p: Any) {
    p.toString()
}

/* GENERATED_FIR_TAGS: break, continue, equalityExpression, forLoop, functionDeclaration, ifExpression, inline,
integerLiteral, lambdaLiteral, localProperty, propertyDeclaration, rangeExpression */
