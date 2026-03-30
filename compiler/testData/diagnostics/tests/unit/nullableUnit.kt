// RUN_PIPELINE_TILL: FRONTEND
fun foo() {}

val x: Unit? = <!NO_ELSE_IN_WHEN!>when<!> ("A") {
    "B" -> foo()
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, nullableType, propertyDeclaration, stringLiteral,
whenExpression, whenWithSubject */
