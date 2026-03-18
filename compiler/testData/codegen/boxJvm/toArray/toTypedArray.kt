// TARGET_BACKEND: JVM
// WITH_STDLIB

fun box(): String {
    val array = listOf(2, 3, 9).toTypedArray()
    if (!array.isArrayOf<Int>()) return "fail: is not Array<Int>"

    val str = array.contentToString()
    if (str != "[2, 3, 9]") return str

    return "OK"
}
