// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_KT_DUMP

// FILE: kt44993.kt
fun f(r: KotlinBox<JavaBox>): String =
    r?.data?.element!!

class KotlinBox<T>(@JvmField val data: T?)

// FILE: JavaBox.java
public class JavaBox {
    public final String element = "OK";
}
