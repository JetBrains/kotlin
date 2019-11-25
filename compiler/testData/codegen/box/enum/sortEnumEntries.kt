// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import Game.*

enum class Game {
    ROCK,
    PAPER,
    SCISSORS,
    LIZARD,
    SPOCK
}

fun box(): String {
    val a = arrayOf(LIZARD, SCISSORS, SPOCK, ROCK, PAPER)
    a.sort()
    val str = a.joinToString(" ")
    return if (str == "ROCK PAPER SCISSORS LIZARD SPOCK") "OK" else "Fail: $str"
}
