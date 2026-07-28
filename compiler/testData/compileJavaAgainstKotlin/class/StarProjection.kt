// FILE: StarProjection.java
package test;

public class StarProjection {
    void foo(K<?> k) {
        k.foo(null);
        StarProjectionKt.bar(null);
        new Sub().foo(null);
    }
}

// FILE: StarProjection.kt
package test

open class K<out T: K<T>> {
    fun foo(k: K<*>) {}
    fun foo(): K<*> = null!!
}

class Sub: K<K<*>>()

fun bar(k: K<*>) {}
