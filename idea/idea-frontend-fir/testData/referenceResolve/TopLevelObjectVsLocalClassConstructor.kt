package test

object Conflict

fun test() {
    class Conflict

    <caret>Conflict()
}

// REF: (in test.test).Conflict