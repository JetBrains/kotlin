// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -NOTHING_TO_INLINE -USELESS_ELVIS

inline fun inlineFunWrongUsage(s: (p: Int) -> Unit) {
    s

    if (true) s else 0

    s ?: s
}

inline fun inlineFunWrongUsageExt(ext: Int.(p: Int) -> Unit) {
    ext

    if (true) ext else 0

    ext ?: ext
}

inline fun inlineFunWrongUsageInClosure(s: (p: Int) -> Unit) {
    <!UNRESOLVED_REFERENCE!>{
        s

        if (true) s else 0

        s ?: s
    }()<!>
}

inline fun inlineFunWrongUsageInClosureExt(ext: Int.(p: Int) -> Unit) {
    <!UNRESOLVED_REFERENCE!>{
        ext

        if (true) ext else 0

        ext ?: ext
    }()<!>
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