fun test(some: Any?, error: Int) {
    some?.let {
        println(some)
    } ?: <caret>println(error)
}

// SET_FALSE: CONTINUATION_INDENT_IN_ELVIS