// TARGET_BACKEND: JVM
// FILE: javaSyntheticGenericPropertyAccess.kt
fun <F> test(j: J<F>) {
    j.foo
    j.foo = 1
    j.foo++
    j.foo += 1
}

// FILE: J.java
public class J<T> {
    private int foo = 42;

    public int getFoo() { return foo; }
    public void setFoo(int x) {}
}