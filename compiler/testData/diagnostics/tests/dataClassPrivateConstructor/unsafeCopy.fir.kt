// WITH_STDLIB
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@OptIn(ExperimentalStdlibApi::class)
@kotlin.SafeCopy
data class Data private constructor(val x: Int)

fun local(data: Data) {
    data.<!DATA_CLASS_COPY_USAGE_WILL_BECOME_INACCESSIBLE_ERROR!>copy<!>()
}
