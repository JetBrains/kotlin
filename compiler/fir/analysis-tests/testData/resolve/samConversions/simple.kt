// FILE: MyRunnable.java
public interface MyRunnable {
    boolean foo(int x);
}

// FILE: JavaUsage.java

public class JavaUsage {
    public static void foo(MyRunnable x) {}
}
// FILE: main.kt

fun foo(m: MyRunnable) {}

fun main() {
    JavaUsage.foo {
        x ->
        x > 1
    }

    JavaUsage.foo({ it > 1 })

    val x = { x: Int -> x > 1 }

    JavaUsage.foo(x)
}
