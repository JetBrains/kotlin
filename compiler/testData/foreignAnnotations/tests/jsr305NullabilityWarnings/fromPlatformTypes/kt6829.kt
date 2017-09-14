// !DIAGNOSTICS: -UNUSED_PARAMETER
// WARNING_FOR_JSR305_ANNOTATIONS

// KT-6829 False warning on map to @Nullable

// FILE: J.java

public class J {

    @MyNullable
    public String method() { return ""; }
}

// FILE: k.kt

fun foo(collection: Collection<J>) {
    val mapped = collection.map { it.method() }
    <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>mapped[0]<!>.length
}

public fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> {
    null!!
}