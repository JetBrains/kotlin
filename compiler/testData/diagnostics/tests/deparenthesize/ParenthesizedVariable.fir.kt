// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    (d@ <!EXPRESSION_EXPECTED!>val bar = 2<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
