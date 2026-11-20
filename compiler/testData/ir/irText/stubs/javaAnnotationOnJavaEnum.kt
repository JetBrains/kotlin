// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: JavaEnum
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.

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
