// TARGET_BACKEND: JVM
// FILE: JavaClass.java
public class JavaClass {
    public static class Nested {}
}

// FILE: test.kt
fun Void.foo(): Boolean {
    return true
}

fun JavaClass.bar(): Boolean {
    return true
}

fun JavaClass.Nested.baz(): Boolean {
    return true
}

fun box(): String {
    return if ((null?.foo() == null) && JavaClass().bar() && JavaClass.Nested().baz()) "OK"
    else "FAIL"
}