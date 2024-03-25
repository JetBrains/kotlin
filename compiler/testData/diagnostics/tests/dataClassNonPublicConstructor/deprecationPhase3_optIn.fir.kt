// WITH_STDLIB
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility
<!REDUNDANT_ANNOTATION!>@ConsistentDataCopyVisibility<!>
data class Data private constructor(val x: Int)

fun usage(data: Data) {
    data.<!INVISIBLE_REFERENCE!>copy<!>()
}
