// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: JavaEnum

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