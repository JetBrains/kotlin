// TARGET_BACKEND: JVM_IR
// JVM_ABI_K1_K2_DIFF: KT-64954
// ISSUE: KT-73339
// FULL_JDK

// FILE: P.java
public class P<T> {
    public final T x = null;
}

// FILE: A.java
import java.util.function.Predicate;

public class A<T> {
    public void f(Predicate<? super T> c) {}
}

// FILE: 1.kt
fun test(a: A<*>) {
    a.f { p ->
        p as P<*>
        p.x
        true
    }
}

fun box(): String {
    test(A<Any>())
    return "OK"
}
