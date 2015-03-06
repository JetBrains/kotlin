// !DIAGNOSTICS: -UNUSED_PARAMETER

// KT-6829 False warning on map to @Nullable

// FILE: p/J.java
package p;

import org.jetbrains.annotations.*;

public class J {

    @Nullable
    public String method() { return ""; }
}

// FILE: k.kt

import p.*

fun foo(collection: Collection<J>) {
    val mapped = collection.map { it.method() }
    <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>mapped[0]<!>.length()
}

public fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> {
    null!!
}