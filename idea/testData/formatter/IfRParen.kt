fun x() {
    if (foo.length > 0 &&
        barLen > 0) {
        println("> 0")
    }

    if (foo.length > 0) {
        println("> 0")
    }
}

// SET_TRUE: IF_RPAREN_ON_NEW_LINE
// SET_FALSE: CONTINUATION_INDENT_IN_IF_CONDITIONS
