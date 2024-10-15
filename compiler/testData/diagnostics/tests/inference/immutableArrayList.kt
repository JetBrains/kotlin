// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: p/J.java
package p;

public interface J {
    public interface Super<T> {}
    public interface Sub<T> extends Super<T> {}
}

// FILE: k.kt

import p.J.*

class Foo<T>: Sub<T> {
    fun foo(): Super<T> {
        return Foo()
    }
}