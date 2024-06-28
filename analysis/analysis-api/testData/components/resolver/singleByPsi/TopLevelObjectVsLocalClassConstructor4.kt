package test

object Conflict

operator fun Conflict.invoke() {}

fun test() {
    class Conflict(i: Int)

    <caret>Conflict()
}

