// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: FRONTEND
fun main(args: Array<String>) {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> passIt = { <!COMPONENT_FUNCTION_MISSING, VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>[t: <!UNRESOLVED_REFERENCE!>T<!>]<!> -> t }
    <!CANNOT_INFER_PARAMETER_TYPE!>passIt<!><!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>(1)
    <!CANNOT_INFER_PARAMETER_TYPE!>passIt<!>(1)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, typeParameter */
