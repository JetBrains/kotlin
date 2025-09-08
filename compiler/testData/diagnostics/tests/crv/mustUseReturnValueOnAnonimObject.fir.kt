// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun test() {
    <!RETURN_VALUE_NOT_USED!>(@MustUseReturnValue object {
        val number: Int = 42
    }).number<!>

    <!RETURN_VALUE_NOT_USED!>(@MustUseReturnValue object {
        fun compute(): Int = 24
    }).compute()<!>
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, integerLiteral, propertyDeclaration */
