// FLOW: OUT

fun test(m: Int, <caret>n: Int) {
    val x = if (m > 1) n else 1
}