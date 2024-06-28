package test

class Conflict {
    companion object
}

fun test() {
    class Conflict

    <caret>Conflict
}