suspend fun callRelease() {
    dummy()

    C().dummy()

    // TODO: This should be error
    WithNested.Nested().dummy()

    // TODO: This should be error
    WithInner().Inner().dummy()
}