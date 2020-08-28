fun a() {
    val b = listOf(fun(c: Int) {})
    b[0](<caret>)
}

// SET_FALSE: ALIGN_MULTILINE_BINARY_OPERATION
