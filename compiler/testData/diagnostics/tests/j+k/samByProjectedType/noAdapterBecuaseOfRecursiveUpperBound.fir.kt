// KT-67021: Cannot find cached type parameter by FIR symbol: E of the owner: FirRegularClassSymbol Function
// SKIP_FIR2IR
// !CHECK_TYPE
// FILE: Function.java
public interface Function<E extends CharSequence, F extends java.util.Map<String, E>> {
    E handle(F f);
}

// FILE: A.java
public class A {
    public void foo(Function<?, ?> l) {
    }

    public static void bar(Function<?, ?> l) {
    }
}

// FILE: main.kt
fun main() {
    A().foo {
        x ->
        ""
    }

    A.bar {
        x ->
        ""
    }
}
