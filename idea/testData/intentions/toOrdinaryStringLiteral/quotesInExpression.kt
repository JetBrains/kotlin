// WITH_RUNTIME
fun test(list: List<Int>) {
    val s = <caret>"""foo ${list.joinToString(", ")} bar"""
}