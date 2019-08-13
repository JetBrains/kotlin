// !LANGUAGE: +NewInference +SamConversionPerArgument +SamConversionForKotlinFunctions
// FILE: samConversionInGenericConstructorCall_NI.kt
fun test3(
    f1: (String) -> String,
    f2: (Int) -> String
) =
    C(f1).D(f2)

class Outer<T1>(val j11: J<T1, T1>) {
    inner class Inner<T2>(val j12: J<T1, T2>)
}

fun test4(f: (String) -> String, g: (Any) -> String) = Outer(f).Inner(g)

fun testGenericJavaCtor1(f: (String) -> Int) = G(f)

fun testGenericJavaCtor2(x: Any) {
    x as (String) -> Int
    G(x)
}

// FILE: J.java
public interface J<T1, T2> {
    T1 foo(T2 x);
}

// FILE: C.java
public class C<X> {
    public C(J<X, X> jxx) {}

    public class D<Y> {
        public D(J<X, Y> jxy) {}
    }
}

// FILE: G.java
public class G<TClass> {
    public <TCtor> G(J<TCtor, TClass> x) {}
}
