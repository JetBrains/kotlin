// HIGHLIGHT: INFO

fun foo(): Int {
    var res = 0
    loop@ while (true) {
        <caret>when (1) {
            1 -> res += 1
            2 -> throw Exception()
            3 -> break@loop
            4 -> continue@loop
            else -> return -1
        }
    }
    return res
}