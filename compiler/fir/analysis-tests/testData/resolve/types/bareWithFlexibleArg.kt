// FIR_IDENTICAL
// SKIP_TXT

// FILE: Promise.java
public interface Promise<T> {}

// FILE: CancellablePromise.java
public interface CancellablePromise<E> extends Promise<E> {}

// FILE: main.kt
fun foo(x: Promise<String?>) {
    bar(x as CancellablePromise)
}
fun bar(x: CancellablePromise<String?>) {}

