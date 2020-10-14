// IS_APPLICABLE: false

class Random {
    fun nextInt(): Int {
        return 42
    }

    fun nextInt(bound: Int): Int {
        return 42
    }
}

fun main() {
    val random: (Int) -> Unit = {<caret> Random().nextInt(it) }
}