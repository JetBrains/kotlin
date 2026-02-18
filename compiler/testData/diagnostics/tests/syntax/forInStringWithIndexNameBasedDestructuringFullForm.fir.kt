// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

fun test(str: String) {
    <!ITERATOR_AMBIGUITY!>for ((<!SYNTAX!><!SYNTAX!><!>var<!> <!SYNTAX!>index<!><!><!SYNTAX!><!SYNTAX!><!>, var value) in str.withIndex())<!> {}

    <!ITERATOR_AMBIGUITY!>for ((<!SYNTAX!><!SYNTAX!><!>var<!> <!SYNTAX!>index<!><!><!SYNTAX!><!SYNTAX!><!>, val value) in str.withIndex())<!> {}

    for ((val index, <!SYNTAX!><!SYNTAX!><!>var<!> <!SYNTAX!>value<!>) <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!><!SYNTAX!><!>in<!> str.withIndex()<!SYNTAX!>)<!> {}
}

/* GENERATED_FIR_TAGS: flexibleType, forLoop, functionDeclaration, javaFunction, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */
