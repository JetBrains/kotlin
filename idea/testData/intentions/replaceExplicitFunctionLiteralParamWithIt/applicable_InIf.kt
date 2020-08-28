fun test(i: Int) {
    val p: (String) -> Boolean =
        if (i == 1) { { <caret>s -> true } } else { { s -> false } }
}