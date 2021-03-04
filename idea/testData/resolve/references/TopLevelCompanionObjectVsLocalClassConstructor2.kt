package test

class Conflict(i: Int) {
    companion object {
        operator fun invoke() {}
    }
}

fun test() {
    class Conflict

    <caret>Conflict()
}

// REF: (in test.test).Conflict