// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun iAmMustUse(x: Boolean): Boolean = x

fun a(c: Int) {
    <!RETURN_VALUE_NOT_USED!>iAmMustUse<!>(run {
        <!RETURN_VALUE_NOT_USED!>iAmMustUse<!>(true)
        true
    })
}

fun b(c: Int) {
    <!RETURN_VALUE_NOT_USED!>iAmMustUse<!>(if (c > 0) {
        <!RETURN_VALUE_NOT_USED!>iAmMustUse<!>(true)
        true
    } else false)
}

fun c(c: Int) {
    <!RETURN_VALUE_NOT_USED!>iAmMustUse<!>(when(c) {
                   0 -> {
                       <!RETURN_VALUE_NOT_USED!>iAmMustUse<!>(true)
                       iAmMustUse(true)
                   }
                   else -> true
               })
}

/* GENERATED_FIR_TAGS: comparisonExpression, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
lambdaLiteral, whenExpression, whenWithSubject */
