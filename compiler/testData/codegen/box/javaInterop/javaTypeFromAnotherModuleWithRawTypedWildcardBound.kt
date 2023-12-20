// TARGET_BACKEND: JVM_IR
// ISSUE: KT-64090

// MODULE: lib
// FILE: Invariant.java
public class Invariant<T> {}

// FILE: Generic.java
public class Generic<T> {
    public class Inner {}
    public static Invariant<? extends Generic.Inner> foo() {
        return null;
    }
}

// MODULE: main(lib)
// FILE: Main.kt
fun box(): String {
    val value = Generic.foo()
    value.bar()
    return "OK"
}

fun <T> T.bar() {}
