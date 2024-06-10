// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
// WITH_STDLIB
data class PrivateInline private constructor(val value: Int) {
    <!NOTHING_TO_INLINE!>inline<!> fun huh() {
        copy()
    }
}

data class PublishedApiInline @PublishedApi internal constructor(val value: Int) {
    <!NOTHING_TO_INLINE!>inline<!> fun huh() {
        copy()
    }
}

data class InternalInline internal constructor(val value: Int) {
    <!NOTHING_TO_INLINE!>inline<!> fun huh() {
        copy()
    }
}
