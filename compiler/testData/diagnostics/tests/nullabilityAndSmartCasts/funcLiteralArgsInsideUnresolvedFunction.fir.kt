// RUN_PIPELINE_TILL: FRONTEND
package a

fun foo() {
    val i : Int? = 42
    if (i != null) {
        <!UNRESOLVED_REFERENCE!>doSmth<!> {
            val x = i + 1
        }
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast */
