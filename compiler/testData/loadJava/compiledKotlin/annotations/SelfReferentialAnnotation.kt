// PLATFORM_DEPENDANT_METADATA
// NO_CHECK_SOURCE_VS_BINARY
// MUTE_REASON: KT-58935

// IGNORE_FIR_METADATA_LOADING_K2_WITH_ANNOTATIONS_IN_METADATA
// ^ With annotations in metadata, compiler also loads annotations on annotation constructor parameters.
// Once AnnotationsInMetadata is enabled by default, this directive can be removed and the txt dump can be updated.

package test

annotation class Ann(@Ann(1) val e: Int)

@MyRequiresOptIn("", MyRequiresOptIn.MyLevel.ERROR)
public annotation class MyRequiresOptIn(
    val a: String = "",
    @MyRequiresOptIn("", MyRequiresOptIn.MyLevel.WARNING) val b: MyLevel = MyLevel.ERROR
) {
    public enum class MyLevel {
        WARNING,
        ERROR,
    }
}
