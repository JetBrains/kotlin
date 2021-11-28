// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -NOTHING_TO_INLINE -USELESS_ELVIS

inline fun inlineFunWrongUsage(s: (p: Int) -> Unit) {
    <!USAGE_IS_NOT_INLINABLE!>s<!>

    if (true) <!USAGE_IS_NOT_INLINABLE!>s<!> else 0

    <!USAGE_IS_NOT_INLINABLE!>s<!> ?: <!USAGE_IS_NOT_INLINABLE!>s<!>
}

inline fun inlineFunWrongUsageExt(ext: Int.(p: Int) -> Unit) {
    <!USAGE_IS_NOT_INLINABLE!>ext<!>

    if (true) <!USAGE_IS_NOT_INLINABLE!>ext<!> else 0

    <!USAGE_IS_NOT_INLINABLE!>ext<!> ?: <!USAGE_IS_NOT_INLINABLE!>ext<!>
}

inline fun inlineFunWrongUsageInClosure(s: (p: Int) -> Unit) {
    {
        <!USAGE_IS_NOT_INLINABLE!>s<!>

        if (true) <!USAGE_IS_NOT_INLINABLE!>s<!> else 0

        <!USAGE_IS_NOT_INLINABLE!>s<!> ?: <!USAGE_IS_NOT_INLINABLE!>s<!>
    }()
}

inline fun inlineFunWrongUsageInClosureExt(ext: Int.(p: Int) -> Unit) {
    {
        <!USAGE_IS_NOT_INLINABLE!>ext<!>

        if (true) <!USAGE_IS_NOT_INLINABLE!>ext<!> else 0

        <!USAGE_IS_NOT_INLINABLE!>ext<!> ?: <!USAGE_IS_NOT_INLINABLE!>ext<!>
    }()
}

inline fun inlineFunNoInline(noinline s: (p: Int) -> Unit) {
    s
    if (true) s else 0

    s ?: s
}

inline fun inlineFunNoInline(noinline ext: Int.(p: Int) -> Unit) {
    ext
    if (true) ext else 0

    ext ?: ext
}
