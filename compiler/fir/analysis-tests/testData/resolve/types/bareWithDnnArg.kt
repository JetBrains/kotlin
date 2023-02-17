// FIR_IDENTICAL
// SKIP_TXT

// FILE: Promise.java
public interface Promise<T> {}

// FILE: main.kt

interface KotlinCancellablePromise<E> : Promise<E>

class D<T> {
    fun foo(x: Promise<T & Any>) {
        bar(x as KotlinCancellablePromise)
    }

    fun bar(x: KotlinCancellablePromise<T & Any>) {}
}
