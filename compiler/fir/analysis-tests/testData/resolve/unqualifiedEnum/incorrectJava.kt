// LANGUAGE: +ExpectedTypeGuidedResolution
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
    first() -> 1
    second -> 2
}
