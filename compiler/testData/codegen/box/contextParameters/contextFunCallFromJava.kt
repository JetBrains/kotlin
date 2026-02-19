// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JVM
// LANGUAGE: +ContextParameters
// FILE: JavaClass.java
public class JavaClass {
    public String test() {
        CallFromJavaTest kotlinClass = new CallFromJavaTest();
        return kotlinClass.foo("", 1, true) + kotlinClass.getBar( "", 1);
    }
}

// FILE: test.kt
class CallFromJavaTest {
    context(a: String)
    fun Int.foo(b: Boolean): String {
        return "O"
    }

    context(a: String)
    val Int.bar: String
        get() = "K"
}

fun box(): String {
    return JavaClass().test()
}