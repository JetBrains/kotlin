// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
// DIAGNOSTICS: -NOTHING_TO_INLINE
// WITH_STDLIB
data class PrivateInline <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_ERROR!>private<!> constructor(val value: Int) {
    inline fun huh1() {
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>PrivateInline<!>(1)
        <!NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE_ERROR!>copy<!>()
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

data class PublishedApiInline @PublishedApi <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_ERROR!>internal<!> constructor(val value: Int) {
    inline fun huh1() {
        PublishedApiInline(1)
        <!NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE_ERROR!>copy<!>()
    }

    internal inline fun huh2() {
        PublishedApiInline(1)
        copy()
    }
}

data class InternalInline <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_ERROR!>internal<!> constructor(val value: Int) {
    inline fun huh1() {
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>InternalInline<!>(1)
        <!NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE_ERROR!>copy<!>()
    }

    internal inline fun huh2() {
        InternalInline(1)
        copy()
    }
}
