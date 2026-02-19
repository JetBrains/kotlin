// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    (<!REDUNDANT_LABEL_WARNING!>d@<!> <!DECLARATION_IN_ILLEGAL_CONTEXT!>val bar = 2<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
