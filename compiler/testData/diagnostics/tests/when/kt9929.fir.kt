// RUN_PIPELINE_TILL: FRONTEND
val test: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> if (true) {
    when (2) {
        1 -> 1
        else -> null
    }
}
else {
    2
}

/* GENERATED_FIR_TAGS: equalityExpression, ifExpression, integerLiteral, propertyDeclaration, whenExpression,
whenWithSubject */
