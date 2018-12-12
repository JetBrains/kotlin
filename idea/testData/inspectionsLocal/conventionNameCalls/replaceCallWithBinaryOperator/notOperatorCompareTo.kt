// PROBLEM: none

fun test(p1: Operation, p2: Operation) {
    p1.<caret>compareTo(p2) < 0
}

class Operation {
    fun compareTo(other: Operation) = 0
}