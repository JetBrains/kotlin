// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun foo(f: Boolean): Int {
    val i: Int
    if (f) {}
    i = 3
    return i
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, ifExpression, integerLiteral, localProperty, propertyDeclaration */
