// MODULE: lib
// KOTLINC_ARGS: -XXLanguage:+ErrorAboutDataClassCopyVisibilityChange -XXLanguage:+DataClassCopyRespectsConstructorVisibility
// FILE: Lib.kt
data class Data private constructor(val value: String)

fun copy(value: String = ""): Data = null!!
class IrrelevantClass {
    fun copy(value: String = ""): Data = null!!
}

// MODULE: main(lib)
// KOTLINC_ARGS: -progressive -XXLanguage:+ErrorAboutDataClassCopyVisibilityChange -XXLanguage:+DataClassCopyRespectsConstructorVisibility
// FILE: main.kt
fun test(irrelevantClass: IrrelevantClass) {
    copy()
    irrelevantClass.copy()
}
