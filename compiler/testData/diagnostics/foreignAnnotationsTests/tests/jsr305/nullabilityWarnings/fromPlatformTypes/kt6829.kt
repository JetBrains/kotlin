// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn

// KT-6829 False warning on map to @Nullable

// FILE: J.java
public class J {

    @MyNullable
    public String method() { return ""; }
}

// FILE: k.kt
fun foo(collection: Collection<J>) {
    val mapped = collection.map { it.method() }
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>mapped[0]<!>.length
}

public fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> {
    null!!
}
