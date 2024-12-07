package test

object Conflict

fun testFoo() {
    class Conflict

    <caret>Conflict()
}
