// WITH_STDLIB

fun box(): String {
    sequenceOf(1, 2, 3, 4, 5)
        .map {
            if (it == 4) error("Evaluated too far")
            it
        }
        .take(3)
        .toList()

    return "OK"
}
