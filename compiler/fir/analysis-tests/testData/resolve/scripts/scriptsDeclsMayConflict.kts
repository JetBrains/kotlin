// RUN_PIPELINE_TILL: FRONTEND

// FILE: script1.kts

val a = 42

// FILE: script2.kts

val a = "42"

// FILE: main.kt

fun foo() =  <!OVERLOAD_RESOLUTION_AMBIGUITY!>a<!>

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, propertyDeclaration, stringLiteral */
