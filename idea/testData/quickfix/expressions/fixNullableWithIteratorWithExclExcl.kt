// "Add non-null asserted (!!) call" "true"
class Some {
    operator fun iterator(): Iterator<Int> = null!!
}

fun foo() {
    val test: Some? = Some()
    for (i in <caret>test) { }
}

/* IGNORE_FIR */