package test

class Conflict

fun testFoo() {
    class Conflict

    <caret>Conflict()
}

// REF: (in test.test).Conflict