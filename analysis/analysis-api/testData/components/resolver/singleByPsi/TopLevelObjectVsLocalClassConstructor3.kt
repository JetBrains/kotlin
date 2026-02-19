package test

object Conflict {
    operator fun invoke() {}
}

fun test() {
    class Conflict(i: Int)

    <caret>Conflict()
}

