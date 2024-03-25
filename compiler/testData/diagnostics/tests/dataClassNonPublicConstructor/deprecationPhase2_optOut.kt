// WITH_STDLIB
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@InconsistentDataCopyVisibility
data class Data private constructor(val x: Int)

fun usage(data: Data) {
    data.copy()
}
