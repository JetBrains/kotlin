fun testIf() {
    if (todo()) 1 else 2
}

fun testIf1(b: Boolean) {
    if (b) todo() else 1

    bar()
}

fun todo(): Nothing = throw Exception()
fun bar() {}