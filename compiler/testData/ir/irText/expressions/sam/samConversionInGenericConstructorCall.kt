// !LANGUAGE: +NewInference +SamConversionPerArgument
// FILE: samConversionInGenericConstructorCall.kt
fun test1(f: (String) -> String) = C(f)

fun test2(x: Any) {
    x as (String) -> String
    C(x)
}

// FILE: J.java
public interface J<T1, T2> {
    T1 foo(T2 x);
}

// FILE: C.java
public class C<X> {
    public C(J<X, X> jxx) {}
}

