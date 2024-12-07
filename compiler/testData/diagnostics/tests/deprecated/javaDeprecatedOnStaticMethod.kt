// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: JavaClass.java
public class JavaClass {
    /**
     * @deprecated Deprecation message
     */
    public static void foo() {}
}

// FILE: main.kt
fun main() {
    JavaClass.<!DEPRECATION!>foo<!>()
}
