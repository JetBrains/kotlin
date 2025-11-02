// RUN_PIPELINE_TILL: FRONTEND
fun foo() {
    var x = 0
    when {
        (1 + 1 == 2) -> x = 1
        (1 + 3) -> x = 2 // must fail here
        else -> X = 3
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, whenExpression */
