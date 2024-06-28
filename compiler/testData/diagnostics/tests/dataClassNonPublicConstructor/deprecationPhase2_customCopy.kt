// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
data class Data private constructor(val x: Int) {
    fun copy() = Data(1)
}

fun local(data: Data) {
    data.copy()
}
