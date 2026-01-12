// RUN_PIPELINE_TILL: BACKEND
// FILE: A.java

class A {
    enum Empty {}

    static Empty foo() { return null; }
}

// FILE: 1.kt

fun bar() = <!WHEN_ON_SEALED!>when (A.foo()) {
    null -> "null"
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> "else"
}<!>

fun <T : Number?> baz(t: T) = when (t) {
    is Int -> "int"
    is Long -> "long"
    null -> "null"
    else -> "else"
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, isExpression, nullableType, smartcast, stringLiteral,
typeConstraint, typeParameter, whenExpression, whenWithSubject */
