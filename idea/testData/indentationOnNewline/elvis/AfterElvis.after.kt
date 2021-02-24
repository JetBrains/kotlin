fun test(some: Any?, error: Int) {
    val test = some ?:
    <caret>error
}

// SET_FALSE: CONTINUATION_INDENT_IN_ELVIS