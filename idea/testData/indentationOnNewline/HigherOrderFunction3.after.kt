fun a(): () -> () -> Unit = { { } }

fun t() {
    a()()(
        <caret>
         )
}

// SET_TRUE: ALIGN_MULTILINE_METHOD_BRACKETS
// IGNORE_FORMATTER