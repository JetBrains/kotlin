// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
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
