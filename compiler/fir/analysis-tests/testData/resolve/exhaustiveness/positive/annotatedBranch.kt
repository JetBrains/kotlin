// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-50550

enum class SomeEnum { A, B}

fun test(x: SomeEnum) {
    <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        SomeEnum.A -> 1
        @Suppress("deprecation")
        SomeEnum.B -> 2
    }<!>.inc()
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, integerLiteral, smartcast,
stringLiteral, whenExpression, whenWithSubject */
