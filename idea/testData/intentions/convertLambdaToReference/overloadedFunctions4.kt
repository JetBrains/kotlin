// IS_APPLICABLE: false
class Random {
    fun nextInt(): Int = 42
    fun nextInt(bound: Int): Int = 42
}

fun main() {
    val random: (Int) -> Int = {<caret> n: Int -> Random().nextInt(n) } + { n: Int -> Random().nextInt(n) }
}

operator fun ((Int) -> Int).plus(other: (Int) -> Int): (Int) -> Int {
    return { n -> this(n) + other(n) }
}