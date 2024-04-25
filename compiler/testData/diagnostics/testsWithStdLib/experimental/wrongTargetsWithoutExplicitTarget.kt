// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn

@RequiresOptIn
annotation class SomeOptInMarker

@RequiresOptIn
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.LOCAL_VARIABLE)
annotation class OtherOptInMarker

class IntWrapper(
    <!OPT_IN_MARKER_ON_WRONG_TARGET!>@SomeOptInMarker<!>
    <!OPT_IN_MARKER_ON_WRONG_TARGET!>@OtherOptInMarker<!>
    val value: Int
) {
    val isEven: Boolean
        <!OPT_IN_MARKER_ON_WRONG_TARGET!>@SomeOptInMarker<!>
        <!OPT_IN_MARKER_ON_WRONG_TARGET!>@OtherOptInMarker<!>
        get() = (value % 2) == 0
}

fun foo() {
    <!OPT_IN_MARKER_ON_WRONG_TARGET!>@SomeOptInMarker<!>
    <!OPT_IN_MARKER_ON_WRONG_TARGET!>@OtherOptInMarker<!>
    val value = 2
}