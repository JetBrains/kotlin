fun a() {
    val somelong = 3 + 4 - (
            <caret>
            )
}

// SET_FALSE: ALIGN_MULTILINE_BINARY_OPERATION
// IGNORE_FORMATTER