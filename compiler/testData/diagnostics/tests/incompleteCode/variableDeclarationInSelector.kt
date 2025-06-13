// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun foo(s: String) {
    s.<!SYNTAX!><!>
    val b = 42
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
