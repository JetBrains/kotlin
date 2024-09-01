// WITH_STDLIB
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@ConsistentCopyVisibility
data class Data private constructor(val x: Int)

fun usage(data: Data) {
    data.copy()
}

@ConsistentCopyVisibility
data class DontFallbackToPrivateToThis<out T> private constructor(val t: T) {
    private fun private(other: DontFallbackToPrivateToThis<T>) {
        other.copy()
    }
}
