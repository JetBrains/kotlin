// IGNORE_FIR

package test

class Conflict {
    companion object
}

fun test() {
    class Conflict

    <caret>Conflict
}

// REF: companion object of (test).Conflict