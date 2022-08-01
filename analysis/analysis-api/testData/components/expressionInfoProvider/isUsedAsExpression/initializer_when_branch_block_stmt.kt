fun test(b: Boolean) {
    val a = when(b) {
        true -> {
            <caret>5
            45
        }
    }
        else -> 0
    }
}