// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun iAmMustUse(x: Boolean): Boolean = x

fun a(c: Int) {
    iAmMustUse(run {
        iAmMustUse(true)
        true
    })
}

fun b(c: Int) {
    iAmMustUse(if (c > 0) {
        iAmMustUse(true)
        true
    } else false)
}

fun c(c: Int) {
    iAmMustUse(when(c) {
                   0 -> {
                       iAmMustUse(true)
                       iAmMustUse(true)
                   }
                   else -> true
               })
}

/* GENERATED_FIR_TAGS: comparisonExpression, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
lambdaLiteral, whenExpression, whenWithSubject */
