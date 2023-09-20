// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn

// FILE: A.java
public class A {
    public static @MyNullable String bar() {
        return null;
    }
}

// FILE: main.kt
fun foo1(): String = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>A.bar()<!>
fun foo2(): String? = A.bar()
fun foo3() = A.bar()
