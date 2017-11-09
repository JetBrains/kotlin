// FLOW: IN

fun test(n: Int) {
    var <caret>x = n
    val y = x
    x = 0
}