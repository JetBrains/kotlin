fun test(b: Boolean) {
    when(b) {
        true -> <caret>5
        else -> 0
    }
}