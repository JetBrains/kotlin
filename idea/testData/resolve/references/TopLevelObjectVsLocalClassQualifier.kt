// IGNORE_FIR
package test

object Conflict

fun test() {
    class Conflict

    <caret>Conflict
}

// REF: (test).Conflict