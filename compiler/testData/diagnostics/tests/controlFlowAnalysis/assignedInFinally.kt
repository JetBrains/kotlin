// RUN_PIPELINE_TILL: CODEGEN
fun test5() {
    var a: Int
    try {
        a = 3
    }
    finally {
        a = 5
    }
    a.hashCode()
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
tryExpression */
