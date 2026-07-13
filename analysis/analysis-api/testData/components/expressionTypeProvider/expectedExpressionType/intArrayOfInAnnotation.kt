annotation class TestAnnotation(
    val values: IntArray
)
@TestAnnotation(values = intArrayOf(<caret>x))
fun test() {}
