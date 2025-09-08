// RUN_PIPELINE_TILL: BACKEND
fun foo() {
    val <!UNUSED_VARIABLE!>x<!> = fun(s: String) {}

    fun nested() {
        val x = fun(i: Int) {}

        x(10)
    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, integerLiteral, localFunction, localProperty,
propertyDeclaration */
