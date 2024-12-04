// JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8
// FULL_JDK
// FILE: J.java

public interface J {
    default void foo() {}
}

// FILE: K.kt

interface K : J

interface MyList<T> : MutableList<T>
interface MySet<E> : MutableSet<E>
interface MyMap<K, V> : MutableMap<K, V>

interface MyMap2<X, Y> : MyMap<X, Y>
