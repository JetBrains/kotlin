// !LANGUAGE: +NewInference

// FILE: Sam.java

public interface Sam<K> {
    Sam.Result<K> compute();

    public static class Result<V> {
        public static <V> Sam.Result<V> create(V value) {}
    }
}

// FILE: Foo.java

public class Foo {
    public static <T> void foo(Sam<T> var1) {
    }
}

// FILE: test.kt

fun test(e: <!UNRESOLVED_REFERENCE!>ErrorType<!>) {
    Foo.foo {
        Sam.Result.create(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>e<!>)
    }
}