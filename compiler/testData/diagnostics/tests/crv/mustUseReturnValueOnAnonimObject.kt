// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun test() {
    (@MustUseReturnValues object {
        val number: Int = 42
    }).number

    (@MustUseReturnValues object {
        fun compute(): Int = 24
    }).compute()
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, integerLiteral, propertyDeclaration */
