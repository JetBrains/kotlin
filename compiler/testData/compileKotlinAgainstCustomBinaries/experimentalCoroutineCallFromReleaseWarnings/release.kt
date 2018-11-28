suspend fun callRelease() {
    builder {}
    builder2 { }

    val x: suspend (Int) -> Unit = {}
    x.start()

    dummy()
    C().dummy()
    WithNested.Nested().dummy()
    WithInner().Inner().dummy()

    suspendAcceptsSuspend {
        callRelease()
    }
}
