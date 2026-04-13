// RUN_PIPELINE_TILL: FRONTEND
fun <T> test(x: Any) {
    <!UNRESOLVED_REFERENCE!>println<!>(<!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!><!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><T><!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, typeParameter */
