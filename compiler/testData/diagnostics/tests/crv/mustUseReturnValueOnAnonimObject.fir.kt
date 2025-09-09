// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun test() {
    (@MustUseReturnValues object {
        val number: Int = 42
    }).<!RETURN_VALUE_NOT_USED!>number<!>

    (@MustUseReturnValues object {
        fun compute(): Int = 24
    }).<!RETURN_VALUE_NOT_USED!>compute<!>()
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, integerLiteral, propertyDeclaration */
