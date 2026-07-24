annotation class TestAnnotation(
    val values: IntArray
)
@TestAnnotation(values = [<caret>x])
fun test() {}
