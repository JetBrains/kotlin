suspend fun callRelease() {
    dummy()

    C().dummy()

    WithNested.Nested().dummy()

    WithInner().Inner().dummy()
}
