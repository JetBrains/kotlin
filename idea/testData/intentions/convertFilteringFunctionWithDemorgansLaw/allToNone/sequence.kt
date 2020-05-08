// WITH_RUNTIME
fun test(seq: Sequence<Int>) {
    val b = seq.all<caret> { it != 1 }
}