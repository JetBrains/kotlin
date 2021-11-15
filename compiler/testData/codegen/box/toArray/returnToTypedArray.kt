// WITH_STDLIB

fun getCopyToArray(): Array<Int> = listOf(2, 3, 9).toTypedArray()

fun box(): String {
    val str = getCopyToArray().contentToString()
    if (str != "[2, 3, 9]") return str

    return "OK"
}
