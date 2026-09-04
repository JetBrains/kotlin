// WITH_STDLIB
fun box(): String {
    var src = sequenceOf(1, 2)
    val s = src.map { it * 10 }
    src = sequenceOf(3)
    val r = s.toList()
    return if (r == listOf(10, 20)) "OK" else "Fail: $r"
}
