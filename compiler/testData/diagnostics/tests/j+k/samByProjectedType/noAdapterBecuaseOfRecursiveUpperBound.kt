// !CHECK_TYPE
// FIR_DUMP
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
    A().foo <!TYPE_MISMATCH!>{
        <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        ""
    }<!>

    A.bar <!TYPE_MISMATCH!>{
        <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        ""
    }<!>
}
