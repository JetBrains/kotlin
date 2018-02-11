fun test(b: Boolean): Int {
    while (true) {
        <caret>return when (b) {
            true -> 1
            else -> return 0
        }
    }
}