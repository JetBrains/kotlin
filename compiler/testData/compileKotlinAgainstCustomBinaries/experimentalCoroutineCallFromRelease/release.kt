suspend fun callRelease() {
    c()
    WithTypeParameter<suspend () -> Unit>()
    returnsSuspend()
    builder {}
    withTypeParameter<suspend () -> Unit>()

    dummy()
    C().dummy()
    WithNested.Nested().dummy()
    WithInner().Inner().dummy()
}
