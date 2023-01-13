// FIR_IDENTICAL
// SKIP_TXT
// FILE: A.java

public class A {
    public static B getB() { return null; }
}

// FILE: B.java

public class B<E> {
    public void foo(java.util.Map<String, String> x) {}
}

// FILE: main.kt
fun main(x: Map<Any, Any>) {
    A.getB().foo(x)
}
