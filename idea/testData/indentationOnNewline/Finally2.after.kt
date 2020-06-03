fun a() {
    try {
        dos()
    } finally {
        <caret>
        a
    }
}

// WITHOUT_CUSTOM_LINE_INDENT_PROVIDER