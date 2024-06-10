// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, +DataClassCopyRespectsConstructorVisibility
// DIAGNOSTICS: -NOTHING_TO_INLINE
// WITH_STDLIB
data class PrivateInline private constructor(val value: Int) {
    inline fun huh1() {
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>PrivateInline<!>(1)
        copy()
    }

    private inline fun huh2() {
        PrivateInline(1)
        copy()
    }

    internal inline fun huh3() {
        PrivateInline(1)
        copy()
    }
}

data class PublishedApiInline @PublishedApi internal constructor(val value: Int) {
    inline fun huh1() {
        PublishedApiInline(1)
        copy()
    }

    internal inline fun huh2() {
        PublishedApiInline(1)
        copy()
    }
}

data class InternalInline internal constructor(val value: Int) {
    inline fun huh1() {
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>InternalInline<!>(1)
        copy()
    }

    internal inline fun huh2() {
        InternalInline(1)
        copy()
    }
}
