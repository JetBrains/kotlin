// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN
// ISSUE: KT-67825

// FILE: Test.kt
fun test(k: K<String>) {
    k.foo(JavaBox(null))
    foo2<String>(<!ARGUMENT_TYPE_MISMATCH!>JavaBox(null)<!>)
    foo3(JavaBox(null))
}

class K<T> {
    fun foo(a: JavaBox<out T>) {}
}

fun <T> foo2(a: JavaBox<out T>) {}

fun foo3(a: JavaBox<out String>) {}

// FILE: JavaBox.java
public class JavaBox<T> {
    public JavaBox(T b) {
        a = b;
    }
    public T a;
}
