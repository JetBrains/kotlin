// "Add non-null asserted (!!) call" "true"
fun <T: Collection<Int>?> foo(c: T) {
    for (i in <caret>c) { }
}

