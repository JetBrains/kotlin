// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-35545
// WITH_STDLIB
// DIAGNOSTICS: -RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_WARNING -RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE

// KT-35545: Inconsistency of errors between property getter and function with implicit return type and non-local return from inlined lambda

val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>x<!>
    get() = run { println("d"); return <!RETURN_TYPE_MISMATCH!>true<!> }

val x1: Boolean
    get() = run { println("d"); return true } // ok

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>d<!>(a: Boolean) = run { println("d"); return <!RETURN_TYPE_MISMATCH!>true<!> }

fun d1(a: Boolean): Boolean = run { println("d"); return true } // ok

/* GENERATED_FIR_TAGS: functionDeclaration, getter, lambdaLiteral, propertyDeclaration, stringLiteral */
