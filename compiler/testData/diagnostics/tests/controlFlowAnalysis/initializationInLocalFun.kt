// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun foo() {
    var x: Int
    fun bar() {
        x = 42
    }
    <!UNINITIALIZED_VARIABLE!>x<!>.hashCode()
    bar()
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, integerLiteral, localFunction, localProperty,
propertyDeclaration */
