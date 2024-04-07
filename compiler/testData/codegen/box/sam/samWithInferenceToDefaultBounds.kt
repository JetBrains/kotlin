// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-67021

// FILE: Function.java
public interface Function<E extends CharSequence, F extends java.util.Map<String, E>> {
    E handle(F f);
}

// FILE: A.java
public class A {
    public String foo(Function<?, ?> l) {
        return (String) l.handle(null);
    }

    public static String bar(Function<?, ?> l) {
        return (String) l.handle(null);
    }
}

// FILE: main.kt
fun box(): String {
    val o = A().foo { x -> "O" }

    val k = A.bar { x -> "K" }
    return o + k
}
