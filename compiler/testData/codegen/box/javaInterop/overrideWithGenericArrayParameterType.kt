// TARGET_BACKEND: JVM
// FILE: J.java

import java.util.List;

public interface J<T> {
    String foo(T t, List<T[]> list);
}

// FILE: 1.kt

class A

class C : J<A> {
    override fun foo(a: A, list: List<Array<A>>?): String = "OK"
}

fun box(): String {
    val c: J<A> = C()
    return c.foo(A(), null)
}
