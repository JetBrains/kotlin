fun a() {
    try {
        dos()
    } finally {
        <caret>
        a
    }
}
