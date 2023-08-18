// FIR_IDENTICAL
// ISSUE: KT-58579
// FILE: Invariant.java
public class Invariant<T> {}

// FILE: Generic.java
public class Generic<T> {
    public class Inner {}
    public static Invariant<? extends Generic.Inner> foo() {
        return null;
    }
}

// FILE: Main.kt
fun main() {
    val value = Generic.foo()
    value.bar()
}

fun <T> T.bar() {}
