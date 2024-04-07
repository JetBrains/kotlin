// WITH_STDLIB
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@ExposedCopyVisibility
data class Data private constructor(val x: Int)

fun usage(data: Data) {
    data.copy()
}
