class WithConstructor(val x: Int) {
    @Suppress("UNUSED")
    cons<caret>tructor(x: Int, y: Int) : this(x)
}
