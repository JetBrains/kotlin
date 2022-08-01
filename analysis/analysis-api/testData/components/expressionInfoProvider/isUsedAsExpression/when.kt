fun test(b: Boolean) {
    <caret>when(b) {
        true -> 5
        else -> 0
    }
}