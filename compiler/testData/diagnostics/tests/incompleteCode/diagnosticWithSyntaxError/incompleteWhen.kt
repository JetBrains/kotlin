// RUN_PIPELINE_TILL: FRONTEND
fun test(a: Any) {
    when (a)<!SYNTAX!><!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, whenExpression, whenWithSubject */
