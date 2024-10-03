// TARGET_BACKEND: JVM
// FILE: JavaClass.java
public class JavaClass {
    public static class Nested {}
}

// FILE: test.kt
val Void.foo: Boolean
    get() = true

val JavaClass.bar: Boolean
    get() = true

val JavaClass.Nested.baz: Boolean
    get() = true

fun box(): String {
    return if ((null?.foo == null) && JavaClass().bar && JavaClass.Nested().baz) "OK"
    else "FAIL"
}
