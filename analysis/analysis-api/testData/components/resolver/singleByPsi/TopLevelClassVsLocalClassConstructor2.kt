package test

class Conflict

fun test() {
    class Conflict(i: Int)

    <caret>Conflict()
}

