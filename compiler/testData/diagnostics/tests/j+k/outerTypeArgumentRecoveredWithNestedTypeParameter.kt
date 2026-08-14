// RUN_PIPELINE_TILL: BACKEND
// A recovered implicit outer type argument which contains a type parameter instead of being one:
// `Mid<E> extends Outer<Box<E>>` makes `Inner` written in `Mid`'s body denote `Outer<Box<E>>.Inner`,
// so `get()` returns `Box<E>`, and `unwrap()` a `String` for a `Mid<String>`. The `E` one level down
// is looked up in the same declaration chain as a top-level one.

// FILE: Box.java
public class Box<X> {
    public X unwrap() { return null; }
}

// FILE: Outer.java
public class Outer<T> {
    public class Inner {
        public T get() { return null; }
    }
}

// FILE: Mid.java
public class Mid<E> extends Outer<Box<E>> {
    public Inner nested() { return null; }
}

// FILE: test.kt
fun consumeString(s: String) {}

fun test(m: Mid<String>) {
    consumeString(m.nested().get().unwrap())
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaType */
