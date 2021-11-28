// TARGET_BACKEND: JVM
// This is really a frontend test that checks loading of compiled Java classes.
// MODULE: lib
// FILE: I.java
public interface I<T> {}

// FILE: J.java
public class J implements I<J.X> {
    public static class X {}
}

// FILE: Z.java
public class Z<T> {
    public T foo(J.X x) { return null; }
}

// MODULE: main(lib)
// FILE: main.kt
class C : Z<Int>()

fun box() = C().foo(null) ?: "OK"
