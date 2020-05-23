class Random {
    fun nextInt(): Int {
        return 42
    }

    fun nextInt(bound: Int): Int {
        return 42
    }
}

fun main() {
    val random = {<caret> i: Int -> Random().nextInt(i) }
}