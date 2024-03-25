// WITH_STDLIB
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility
data class Data private constructor(val x: Int)

fun usage(data: Data) {
    data.copy()
}
