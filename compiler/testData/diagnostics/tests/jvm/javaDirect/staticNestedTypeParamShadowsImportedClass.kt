// RUN_PIPELINE_TILL: FRONTEND

// javac divergence — outer type parameter shadowing an imported class inside a STATIC nested type.
//
// Per JLS 6.5.5/8.1.3 the outer class's type parameter `E` is NOT in scope inside the
// static nested type `Outer.Inner`, so for javac the simple name `E` in `Inner.get()`
// denotes the imported class `pkg.E`. Both the PSI Java model and java-direct instead
// resolve `E` to `Outer`'s type parameter, so the Kotlin compiler sees `Inner.get()` as
// returning the (out-of-scope) type variable rather than `pkg.E`. As a result the call
// `inner.get().onlyOnImportedE()` — which compiles in javac — is rejected by Kotlin.

// FILE: pkg/E.java
package pkg;

public class E {
    public int onlyOnImportedE() { return 42; }
}

// FILE: outer/Outer.java
package outer;

import pkg.E;

public class Outer<E> {
    public static class Inner {
        public E get() { return null; }
    }
}

// FILE: main.kt
package main

import outer.Outer

fun test(inner: Outer.Inner) {
    inner.get().<!UNRESOLVED_REFERENCE!>onlyOnImportedE<!>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
