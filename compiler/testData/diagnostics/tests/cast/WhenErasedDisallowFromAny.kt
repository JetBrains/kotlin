// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

fun ff(l: Any) = when(l) {
    is <!CANNOT_CHECK_FOR_ERASED!>MutableList<String><!> -> 1
    else <!SYNTAX!>2<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, isExpression, whenExpression, whenWithSubject */
