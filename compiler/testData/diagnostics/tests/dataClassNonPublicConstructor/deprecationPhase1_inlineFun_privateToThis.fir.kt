// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
// DIAGNOSTICS: -NOTHING_TO_INLINE
// WITH_STDLIB
data class Data<out T> <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING!>private<!> constructor(val t: T) {
    inline fun foo(other: Data<String>) {
        other.<!NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE_WARNING!>copy<!>()
        <!NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE_WARNING!>copy<!>()
    }
}
