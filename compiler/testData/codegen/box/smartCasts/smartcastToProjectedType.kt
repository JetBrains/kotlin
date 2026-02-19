// TARGET_BACKEND: JVM_IR
// ISSUE: KT-73339
// FULL_JDK
// DUMP_IR

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
