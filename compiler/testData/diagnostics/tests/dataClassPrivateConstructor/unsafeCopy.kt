// FIR_IDENTICAL
// WITH_STDLIB
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@OptIn(ExperimentalStdlibApi::class)
@kotlin.UnsafeCopy
data class Data private constructor(val x: Int)

fun local(data: Data) {
    data.<!DATA_CLASS_COPY_WILL_BECOME_INACCESSIBLE_ERROR!>copy<!>()
}
