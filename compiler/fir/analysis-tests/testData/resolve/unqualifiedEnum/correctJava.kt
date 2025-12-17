// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// FILE: JavaEnum.java

public enum JavaEnum {
    FIRST,
    SECOND;
}

// FILE: test.kt

fun foo(javaEnum: JavaEnum) = <!WHEN_ON_SEALED_GEEN_ELSE!>when (javaEnum) {
    FIRST -> 1
    SECOND -> 2
}<!>

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, javaProperty, javaType, smartcast,
whenExpression, whenWithSubject */
