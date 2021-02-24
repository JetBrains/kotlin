fun a(): () -> () -> Unit = { { } }

fun t() {
    a()()(
        <caret>
    )
}

// SET_FALSE: ALIGN_MULTILINE_METHOD_BRACKETS