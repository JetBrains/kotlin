// WITH_STDLIB
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@kotlin.ConsistentCopyVisibility
data class Data private constructor(val x: Int)

fun local(data: Data) {
    data.copy()
}
