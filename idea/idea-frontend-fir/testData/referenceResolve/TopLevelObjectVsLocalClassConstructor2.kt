package test

object Conflict {
    operator fun invoke() {}
}

fun testFoo() {
    class Conflict

    <caret>Conflict()
}

// REF: (in test.test).Conflict