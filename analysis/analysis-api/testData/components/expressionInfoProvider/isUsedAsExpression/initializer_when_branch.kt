fun test(b: Boolean) {
    val a = when(b) {
        true -> <caret>5
        else -> 0
    }
}