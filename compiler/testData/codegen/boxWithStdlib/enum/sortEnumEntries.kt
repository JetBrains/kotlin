import Game.*

enum class Game {
    ROCK,
    PAPER,
    SCISSORS,
    LIZARD,
    SPOCK
}

fun box(): String {
    val a = array(LIZARD, SCISSORS, SPOCK, ROCK, PAPER)
    a.sort()
    val str = a.joinToString(" ")
    return if (str == "ROCK PAPER SCISSORS LIZARD SPOCK") "OK" else "Fail: $str"
}
