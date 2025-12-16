// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: JavaEnum

// Minor bug: some parameter types' flexibility mismatch
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: test.kt
fun foo() {
    val x = JavaEnum.FOO
}

// FILE: JavaPropertyAnnotation.java
public @interface JavaPropertyAnnotation {}

// FILE: JavaEnum.java
public enum JavaEnum {
    @JavaPropertyAnnotation FOO;
}