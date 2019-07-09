// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun getCopyToArray(): Array<Int> = listOf(2, 3, 9).toTypedArray()

fun box(): String {
    val str = getCopyToArray().contentToString()
    if (str != "[2, 3, 9]") return str

    return "OK"
}
