fun testReturn() {
    return todo()
}

fun todo(): Nothing = throw Exception()