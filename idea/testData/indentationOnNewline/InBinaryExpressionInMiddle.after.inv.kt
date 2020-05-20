fun some() {
    val test = 3 +
            <caret>4
}

// SET_TRUE: ALIGN_MULTILINE_BINARY_OPERATION
// WITHOUT_CUSTOM_LINE_INDENT_PROVIDER