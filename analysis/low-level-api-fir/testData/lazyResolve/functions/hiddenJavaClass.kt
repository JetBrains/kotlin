// SKIP_WHEN_OUT_OF_CONTENT_ROOT
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
    fun ch<caret>eck(n: Nested): String = n.foo()
}
