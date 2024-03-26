// SKIP_KT_DUMP
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-66067
// SCOPE_DUMP: A:foo
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// FILE: Java1.java
public class Java1<T extends Number> {
    public void foo(T t) {
    }
}

// FILE: Java2.java
public class Java2 extends KotlinClass {
    public void foo(Object t) {
    }
}

// FILE: 1.kt
class A : Java2()   // Kotlin ← Java ← Kotlin ← Java

open class KotlinClass<T> : Java1<T>() where T: Number
