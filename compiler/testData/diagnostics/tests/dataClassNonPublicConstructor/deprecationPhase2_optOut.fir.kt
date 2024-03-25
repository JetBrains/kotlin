// WITH_STDLIB
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@InconsistentDataCopyVisibility
data class Data private constructor(val x: Int)

fun usage(data: Data) {
    data.<!DATA_CLASS_COPY_USAGE_WILL_BECOME_INACCESSIBLE_ERROR!>copy<!>()
}
