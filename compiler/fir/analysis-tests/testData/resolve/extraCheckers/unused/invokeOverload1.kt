// RUN_PIPELINE_TILL: BACKEND
fun foo() {
    val x = fun(s: String) {}

    fun nested() {
        val <!UNUSED_VARIABLE!>x<!> = fun(i: Int) {}

        x("hello")
    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, localFunction, localProperty, propertyDeclaration,
stringLiteral */
