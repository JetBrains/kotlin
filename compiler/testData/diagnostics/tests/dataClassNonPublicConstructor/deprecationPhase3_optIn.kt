// WITH_STDLIB
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility
@ConsistentDataCopyVisibility
data class Data private constructor(val x: Int)

fun usage(data: Data) {
    data.copy()
}
