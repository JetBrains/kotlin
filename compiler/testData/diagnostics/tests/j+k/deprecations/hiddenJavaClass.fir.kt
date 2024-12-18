// RUN_PIPELINE_TILL: BACKEND
// FILE: JavaClass.java
public class JavaClass {
    @kotlin.Deprecated(message = "nested", level = kotlin.DeprecationLevel.HIDDEN)
    public static class Nested { }
}

// FILE: main.kt
class Nested {
    fun foo() = "OK"
}

class MyClass : JavaClass() {
    fun check(n: Nested): String = n.foo()
}

fun box() = MyClass().check(Nested())
