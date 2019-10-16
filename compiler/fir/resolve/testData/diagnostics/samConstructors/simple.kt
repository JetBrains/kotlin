// FILE: MyRunnable.java
public interface MyRunnable {
    boolean foo(int x);
}

// FILE: main.kt

fun foo(m: MyRunnable) {}

fun main() {
    foo(MyRunnable { x ->
        x > 1
    })

    foo(MyRunnable({ it > 1 }))

    val x = { x: Int -> x > 1 }

    foo(MyRunnable(x))
}
