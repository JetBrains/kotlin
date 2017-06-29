fun sign(x: Int): Int {
    <caret>return when {
        x < 0 -> -1
        x > 0 -> 1
        else -> 0
    }
}