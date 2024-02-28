// FIR_IDENTICAL
// WITH_STDLIB
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@OptIn(ExperimentalStdlibApi::class)
@kotlin.SafeCopy
data class Data private constructor(val x: Int)

fun local(data: Data) {
    data.copy()
}
