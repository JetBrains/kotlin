// !DIAGNOSTICS: -UNUSED_PARAMETER

// KT-6829 False warning on map to @Nullable

// FILE: J.java

import org.jetbrains.annotations.*;

public class J {

    @Nullable
    public String method() { return ""; }
}

// FILE: k.kt

fun foo(collection: Collection<J>) {
    val mapped = collection.map { it.method() }
    mapped[0]<!UNSAFE_CALL!>.<!>length
}

public fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> {
    null!!
}