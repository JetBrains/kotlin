fun a() {
    println(1 + null ?:
    <caret>2)
}

// SET_FALSE: CONTINUATION_INDENT_IN_ELVIS