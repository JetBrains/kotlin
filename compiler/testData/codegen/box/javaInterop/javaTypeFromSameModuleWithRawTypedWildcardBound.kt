// TARGET_BACKEND: JVM_IR
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
fun box(): String {
    val value = Generic.foo()
    value.bar()
    return "OK"
}

fun <T> T.bar() {}
