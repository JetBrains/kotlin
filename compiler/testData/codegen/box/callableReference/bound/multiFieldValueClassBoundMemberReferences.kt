// LANGUAGE: +FullValueClasses
// ISSUE: KT-86207

value class Point(val x: Int, val y: Int) {
    fun render(prefix: String): String = "$prefix:$x:$y"
}

fun box(): String {
    val point = Point(1, 2)

    val memberReference: (String) -> String = point::render
    val memberResult = memberReference("P")
    if (memberResult != "P:1:2") return "FAIL member"

    val propertyReference: () -> Int = point::x
    val propertyResult = propertyReference()
    if (propertyResult != 1) return "FAIL property"

    return "OK"
}
