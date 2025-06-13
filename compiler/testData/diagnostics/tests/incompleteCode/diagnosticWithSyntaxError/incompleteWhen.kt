// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun test(a: Any) {
    when (a)<!SYNTAX!><!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, whenExpression, whenWithSubject */
