// SAM_CONVERSIONS: CLASS
// WITH_SIGNATURES
// FILE: t.kt
fun test() {
    J.f { it }
}

// FILE: UnaryOperator.java
public interface UnaryOperator<T> {
    T apply(T t);
}

// FILE: J.java
public class J {
    public static void f(UnaryOperator<String> s) {}
}
