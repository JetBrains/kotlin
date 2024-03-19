// WITH_STDLIB
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@kotlin.ConsistentDataCopyVisibility
data class Data private constructor(val x: Int)

fun local(data: Data) {
    data.copy()
}
