// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

@RequiresOptIn
annotation class SomeOptInMarker

@RequiresOptIn
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.LOCAL_VARIABLE)
annotation class OtherOptInMarker

class IntWrapper(
    <!EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET!>@SomeOptInMarker<!>
    <!EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET!>@OtherOptInMarker<!>
    val value: Int
) {
    val isEven: Boolean
        <!EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET!>@SomeOptInMarker<!>
        <!EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET!>@OtherOptInMarker<!>
        get() = (value % 2) == 0
}

fun foo() {
    <!EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET!>@SomeOptInMarker<!>
    <!EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET!>@OtherOptInMarker<!>
    val value = 2
}