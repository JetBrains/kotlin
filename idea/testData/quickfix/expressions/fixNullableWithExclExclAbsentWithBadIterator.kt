// "Add non-null asserted (!!) call" "false"
// ACTION: Replace with a 'forEach' function call
// ERROR: For-loop range must have an iterator() method

class Some {
    fun iterator(): Iterator<Int> = null!!
}

fun foo() {
    val test: Some? = Some()
    for (i in <caret>test) { }
}
