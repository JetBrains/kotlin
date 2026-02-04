// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

fun test(str: String) {
    for ((val <!REDECLARATION!>index<!>, val <!REDECLARATION!>index<!> = value) in str.withIndex()) {}

    for ((val <!REDECLARATION!>i<!> = index, val <!REDECLARATION!>i<!> = value) in str.withIndex()) {}
}

/* GENERATED_FIR_TAGS: flexibleType, forLoop, functionDeclaration, javaFunction, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */
