// FILE: A.java

import org.jetbrains.annotations.NotNull;
public interface A<T> {
    void foo(@NotNull T x);
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
    C.create().foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    C.create().foo("")

    C<String>().foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    C<String?>().foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    C<String?>().foo("")
}
