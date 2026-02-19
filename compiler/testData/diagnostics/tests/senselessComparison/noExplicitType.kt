// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun readLine() = "x"

fun foo() {
    var line = ""

    while (<!SENSELESS_COMPARISON!>line != null<!>) {
        line = readLine()

        if (<!SENSELESS_COMPARISON!>line != null<!>) {
            bar()
        }
    }
}

fun bar() {}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, localProperty,
propertyDeclaration, stringLiteral, whileLoop */
