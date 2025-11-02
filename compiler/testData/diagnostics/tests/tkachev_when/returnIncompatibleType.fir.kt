// RUN_PIPELINE_TILL: FRONTEND

var x = 1
var r: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> when (x) {
    0 -> 0
    1 -> "-1"
    else -> 2
}

/* GENERATED_FIR_TAGS: equalityExpression, integerLiteral, propertyDeclaration, stringLiteral, whenExpression,
whenWithSubject */
