// "Add non-null asserted (!!) call" "true"
fun foo() {
    val test : Collection<Int>? = null!!
    for (i in <caret>test) { }
}

