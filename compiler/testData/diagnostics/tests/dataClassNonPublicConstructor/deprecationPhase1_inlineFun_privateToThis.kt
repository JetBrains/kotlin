// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
// DIAGNOSTICS: -NOTHING_TO_INLINE
// WITH_STDLIB
data class Data<out T> private constructor(val t: T) {
    inline fun foo(other: Data<String>) {
        other.copy()
        copy()
    }
}
