// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidClassLiteralWithPotentiallyNullableReifiedLhs
// ISSUE: KT-81385
inline fun <reified T : Int?, reified V> foo(t: T, v: V, nullableT: T?) {
    <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS_WARNING!>t<!>::class
    <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS_WARNING!>v<!>::class
    <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>nullableT<!>::class
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, inline, nullableType, reified, typeParameter */
