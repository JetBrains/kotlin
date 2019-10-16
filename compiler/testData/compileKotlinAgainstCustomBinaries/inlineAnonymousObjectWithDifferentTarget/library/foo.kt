inline fun foo(crossinline x: () -> Unit): Any = object {
    init {
        x()
    }
}
