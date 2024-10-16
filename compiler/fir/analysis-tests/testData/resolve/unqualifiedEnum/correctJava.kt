// LANGUAGE: +ExpectedTypeGuidedResolution
// FILE: JavaEnum.java

public enum JavaEnum {
    FIRST,
    SECOND;
}

// FILE: test.kt

fun foo(javaEnum: JavaEnum) = when (javaEnum) {
    _.FIRST -> 1
    _.SECOND -> 2
}
