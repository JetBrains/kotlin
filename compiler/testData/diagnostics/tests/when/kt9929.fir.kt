val test: Int = if (true) {
    <!TYPE_MISMATCH!>when (2) {
        1 -> 1
        else -> null
    }<!>
}
else {
    2
}
