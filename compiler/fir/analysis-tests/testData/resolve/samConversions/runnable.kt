// FILE: JavaClass.java
public class JavaClass {
    public static void foo(Runnable x) {}
    public void bar(Runnable x) {}
}

// FILE: main.kt
fun main() {
    JavaClass.foo {
        ""
    }

    JavaClass().bar {
        ""
    }
}
