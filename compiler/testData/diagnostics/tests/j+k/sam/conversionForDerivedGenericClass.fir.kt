// FILE: A.java
public interface A<T> {
    void f(T arg);
}

// FILE: B.java
public interface B extends A<Runnable> {}

// FILE: C.java
public class C<K> {
    public void f(K k) {}
    public static <R> void g(R r) {}
}

// FILE: test.kt
fun test(a: A<Runnable>, b: B, c: C<Runnable>) {
    a.f { }
    b.f { }
    c.f { }
    C<Runnable>().f { }
    C.g<Runnable> <!TYPE_MISMATCH!>{ }<!>
}
