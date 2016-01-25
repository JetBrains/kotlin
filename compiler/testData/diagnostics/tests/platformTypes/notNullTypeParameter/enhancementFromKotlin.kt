// FILE: A.kt

import org.jetbrains.annotations.NotNull;
public interface A<T> {
    fun foo(x: T)
}

// FILE: B.java
public class B<E> {
    public void foo(E x) {}
}

// FILE: C.java
public class C<F> extends B<F> implements A<F> {
    public static C<String> create() { return null; }
    public void foo(F x) {}
}

// FILE: main.kt

fun test() {
    C.create().foo(null)
    C.create().foo("")

    C<String>().foo(null)
    C<String?>().foo(null)
    C<String?>().foo("")
}
