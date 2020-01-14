// SKIP_TXT
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: JavaClass.java
public class JavaClass<R> {
    public static String baz(int x) { return ""; }
}

// FILE: main.kt
fun foo(x: (Int) -> String) {}

fun main() {
    foo(JavaClass<*>::baz)
}
