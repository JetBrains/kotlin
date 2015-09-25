fun foo(p: String?, errorHandler: () -> Nothing) {
    if (p == null) {
        errorHandler()
    }

    <caret>p.length()
}
