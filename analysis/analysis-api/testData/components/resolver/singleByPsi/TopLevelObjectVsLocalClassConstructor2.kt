package test

object Conflict {
    operator fun invoke() {}
}

fun testFoo() {
    class Conflict

    <caret>Conflict()
}

