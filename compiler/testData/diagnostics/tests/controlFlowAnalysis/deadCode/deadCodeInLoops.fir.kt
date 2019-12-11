fun testFor() {
    operator fun Nothing.iterator() = (0..1).iterator()

    for (i in todo()) {}
}

fun testWhile() {
    while (todo()) {
    }
}

fun testDoWhile() {
    do {

    } while(todo())

    bar()
}

fun todo(): Nothing = throw Exception()
fun bar() {}