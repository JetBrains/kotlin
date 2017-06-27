fun foo(): Int {
    loop@ while (true) {
        <caret>when (1) {
            1 -> return 1
            2 -> throw Exception()
            3 -> break@loop
            4 -> continue@loop
            else -> return -1
        }
    }
    return 0
}