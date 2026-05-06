// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// FILE: JavaEnum.java

public enum JavaEnum {
    FIRST,
    SECOND;

    public static JavaEnum first() {
        return FIRST;
    }

    public static JavaEnum second = SECOND;
}

// FILE: test.kt

fun foo(javaEnum: JavaEnum) = <!NO_ELSE_IN_WHEN!>when<!> (javaEnum) {
    <!UNRESOLVED_REFERENCE!>first<!>() -> 1
    second -> 2
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, integerLiteral, javaProperty, javaType,
whenExpression, whenWithSubject */
