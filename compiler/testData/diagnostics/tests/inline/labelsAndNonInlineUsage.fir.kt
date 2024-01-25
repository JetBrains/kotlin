// !DIAGNOSTICS: -REDUNDANT_LABEL_WARNING -UNDERSCORE_IS_RESERVED -MULTIPLE_LABELS_ARE_FORBIDDEN

// See KT-65337
// !DIAGNOSTICS: -UNRESOLVED_REFERENCE

// KT-65319
inline fun inline(s: () -> Unit) {}
fun noInline(s: () -> Unit) {}

inline fun bar(s: () -> Unit) {
    inline(s)
    noInline(<!USAGE_IS_NOT_INLINABLE!>s<!>)

    inline(l@ s)
    noInline(l@ <!USAGE_IS_NOT_INLINABLE!>s<!>)

    inline(l2@ l1@ s)
    noInline(l2@ l1@ <!USAGE_IS_NOT_INLINABLE!>s<!>)

    inline(_@ s)
    noInline(_@ <!USAGE_IS_NOT_INLINABLE!>s<!>)

    inline(__@ _@ s)
    noInline(__@ _@ <!USAGE_IS_NOT_INLINABLE!>s<!>)

    s()
    (l@ s)()
    (l2@ l1@ s)()
    (_@ s)()
    (__@ _@ s)()

    s.invoke()
    (l@ s).invoke()
    (l2@ l1@ s).invoke()
    (_@ s).invoke()
    (__@ _@ s).invoke()
}
