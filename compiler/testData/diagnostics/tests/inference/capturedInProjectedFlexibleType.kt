// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: A.java

public class A<T> {
    public A<? super T> getSuperA() { return null; }
}

// FILE: main.kt

fun <T : Any> foo(x: T?, func: (T) -> T?) {}

fun test(a: A<*>) {
    foo(a) { it.superA }
}