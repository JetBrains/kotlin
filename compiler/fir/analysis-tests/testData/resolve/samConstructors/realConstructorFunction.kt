// FILE: MyRunnable.java
public interface MyRunnable {
    boolean foo(int x);
}

// FILE: main.kt

fun foo(m: MyRunnable) {}

fun MyRunnable(x: (Int) -> Boolean) = 1

fun main() {
    foo(<!ARGUMENT_TYPE_MISMATCH!>MyRunnable { x ->
        x > 1
    }<!>)

    foo(<!ARGUMENT_TYPE_MISMATCH!>MyRunnable({ it > 1 })<!>)

    val x = { x: Int -> x > 1 }

    foo(<!ARGUMENT_TYPE_MISMATCH!>MyRunnable(x)<!>)
}
