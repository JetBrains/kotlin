// LANGUAGE: +ContextSensitiveEnumResolutionInWhen
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
    <!UNRESOLVED_REFERENCE!>second<!> -> 2
}
