enum class Game {
    ROCK
    PAPER
    SCISSORS

    default object {
        fun foo() = ROCK
        val bar = PAPER
    }
}

fun box(): String {
    if (Game.foo() != Game.ROCK) return "Fail 1"
    // TODO: fix initialization order and uncomment (KT-5761)
    // if (Game.bar != Game.PAPER) return "Fail 2: ${Game.bar}"
    if (Game.values().size() != 3) return "Fail 3"
    if (Game.valueOf("SCISSORS") != Game.SCISSORS) return "Fail 4"
    return "OK"
}
