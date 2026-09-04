// WITH_STDLIB

fun box(): String {
    var index = 0
    val list = listOf("a", "b", "c")
    sequenceOf("a", "b", "c").withIndex().forEach { (i, v) -> if ((i != index) || (list[index++] != v)) return "fail" }
    return "OK"
}
