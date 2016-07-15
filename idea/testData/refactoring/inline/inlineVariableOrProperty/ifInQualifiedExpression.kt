// WITH_RUNTIME
fun boo() : Pair<Int, String> {
    val flag = true
    val <caret>value = if (flag) 0 else 1
    return value.to("value")
}