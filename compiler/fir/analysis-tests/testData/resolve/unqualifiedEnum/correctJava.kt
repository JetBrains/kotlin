// LANGUAGE: +ContextSensitiveEnumResolutionInWhen
// FILE: JavaEnum.java

public enum JavaEnum {
    FIRST,
    SECOND;
}

// FILE: test.kt

fun foo(javaEnum: JavaEnum) = when (javaEnum) {
    FIRST -> 1
    SECOND -> 2
}
