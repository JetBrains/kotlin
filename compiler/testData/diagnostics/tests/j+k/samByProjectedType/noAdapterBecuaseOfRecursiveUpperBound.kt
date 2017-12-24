// !WITH_NEW_INFERENCE
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
    A().foo <!TYPE_MISMATCH!>{
        <!OI;CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>x<!> ->
        ""
    }<!>

    A.bar <!TYPE_MISMATCH!>{
        <!OI;CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>x<!> ->
        ""
    }<!>
}
