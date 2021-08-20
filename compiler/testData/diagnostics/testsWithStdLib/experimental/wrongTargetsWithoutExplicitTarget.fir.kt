// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

@RequiresOptIn
annotation class SomeOptInMarker

@RequiresOptIn
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.LOCAL_VARIABLE)
annotation class OtherOptInMarker

class IntWrapper(
    @SomeOptInMarker
    @OtherOptInMarker
    val value: Int
) {
    val isEven: Boolean
        @SomeOptInMarker
        @OtherOptInMarker
        get() = (value % 2) == 0
}

fun foo() {
    @SomeOptInMarker
    @OtherOptInMarker
    val value = 2
}